package com.bancada.service;

import com.bancada.enums.ConnectionType;
import com.bancada.enums.DeviceEventType;
import com.bancada.enums.DeviceStatus;
import com.bancada.exception.DeviceAuthenticationException;
import com.bancada.exception.DeviceConnectionException;
import com.bancada.filter.DeviceFilter;
import com.bancada.models.Credential;
import com.bancada.models.Device;
import com.bancada.records.CommandResult;
import com.bancada.records.DeviceFacts;
import com.bancada.records.HostKeyProbe;
import com.bancada.records.KeyValueOutput;
import com.bancada.repository.DeviceRepository;
import com.bancada.request.DeviceRequest;
import com.bancada.response.DeviceMetricsResponse;
import com.bancada.specification.DeviceSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceService.class);
    private static final Duration SCRIPT_TIMEOUT = Duration.ofSeconds(30);
    private static final long MAX_CLOCK_DRIFT_SECONDS = 120;
    private static final DateTimeFormatter UTC_CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceRepository deviceRepository;
    private final CredentialService credentialService;
    private final SshService sshService;
    private final DeviceScriptService deviceScriptService;
    private final EventService eventService;

    public DeviceService(DeviceRepository deviceRepository, CredentialService credentialService, SshService sshService,
                         DeviceScriptService deviceScriptService, EventService eventService) {
        this.deviceRepository = deviceRepository;
        this.credentialService = credentialService;
        this.sshService = sshService;
        this.deviceScriptService = deviceScriptService;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public Page<Device> search(DeviceFilter filter, Pageable pageable) {
        Specification<Device> specification = Specification.where(DeviceSpecification.search(filter.getSearch()))
            .and(DeviceSpecification.statusIn(filter.getStatus()))
            .and(DeviceSpecification.online(filter.getOnline()))
            .and(DeviceSpecification.active(filter.getActive()));
        return deviceRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Device findById(Long id) {
        return deviceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado para ID: " + id));
    }

    /** Registers a device by address. The SSH host key is read first: it is what identifies the device. */
    public Device createManual(DeviceRequest request) {
        HostKeyProbe probe = sshService.probeHostKey(request.host().trim(), request.port());
        if (probe == null) {
            throw new DeviceConnectionException("Nenhum servidor SSH respondeu em " + request.host() + ":" + request.port() + ".");
        }
        deviceRepository.findByHostKeyFingerprint(probe.fingerprint()).ifPresent(existing -> {
            throw new DataIntegrityViolationException("Este dispositivo já está cadastrado como \"" + existing.getName() + "\".");
        });
        Credential credential = request.credentialId() == null ? null : credentialService.findById(request.credentialId());
        Device device = new Device(request.host().trim(), request.port(), probe.fingerprint(), probe.keyType(),
            ConnectionType.NETWORK, null);
        device.update(request, credential);
        Device saved = deviceRepository.save(device);
        return credential == null ? saved : refreshFactsQuietly(saved.getId());
    }

    public Device update(Long id, DeviceRequest request) {
        Device device = findById(id);
        Credential credential = request.credentialId() == null ? null : credentialService.findById(request.credentialId());
        device.update(request, credential);
        if (credential == null) {
            device.changeStatus(DeviceStatus.DISCOVERED);
        }
        sshService.invalidate(id);
        Device saved = deviceRepository.save(device);
        return credential == null ? saved : refreshFactsQuietly(saved.getId());
    }

    @Transactional
    public Device changeActive(Long id, boolean active) {
        Device device = findById(id);
        device.setActive(active);
        if (!active) {
            sshService.invalidate(id);
        }
        return deviceRepository.save(device);
    }

    /** Logs in, reads the system information and marks the device ready. Auth failures are recorded. */
    public Device refreshFacts(Long id) {
        Device device = findById(id);
        CommandResult result;
        try {
            result = sshService.run(device, deviceScriptService.load("facts"), List.of(), false, SCRIPT_TIMEOUT);
        } catch (DeviceAuthenticationException exception) {
            if (device.getCredential() != null) {
                device.changeStatus(DeviceStatus.AUTH_FAILED);
                deviceRepository.save(device);
            }
            throw exception;
        }
        KeyValueOutput output = KeyValueOutput.parse(result.output());
        device.applyFacts(new DeviceFacts(output.text("hostname"), output.text("osName"), output.text("osVersion"),
            output.text("kernelVersion"), output.text("architecture"), output.text("cpuModel"), output.intValue("cpuCores"),
            output.longValue("memoryTotalBytes"), output.longValue("diskTotalBytes"), output.text("model"),
            output.text("macAddress"), output.text("packageManager"), output.text("initSystem"), output.text("homeDirectory"), output.flag("root")));
        device.changeStatus(DeviceStatus.READY);
        Device saved = deviceRepository.save(device);
        syncClock(saved, output.longValue("epochSeconds"));
        eventService.publish(DeviceEventType.UPDATED, saved.getId(), saved.getName(), null, "Informações de " + saved.getName() + " atualizadas");
        return saved;
    }

    /**
     * Phones without a working RTC boot in 1970, and with the wrong date every HTTPS certificate is
     * "not yet valid": apk, curl and git stop working. NTP does not pass through the HTTP proxy the
     * devices use, so the panel sets the clock from this computer when it drifts too far.
     */
    private void syncClock(Device device, Long deviceEpoch) {
        long now = Instant.now().getEpochSecond();
        if (deviceEpoch == null || Math.abs(now - deviceEpoch) <= MAX_CLOCK_DRIFT_SECONDS) {
            return;
        }
        String utc = UTC_CLOCK.format(Instant.ofEpochSecond(now).atOffset(ZoneOffset.UTC));
        try {
            CommandResult result = sshService.run(device, "date -u -s '" + utc + "' >/dev/null && (hwclock -w 2>/dev/null || true)",
                List.of(), true, SCRIPT_TIMEOUT);
            if (result.succeeded()) {
                LOG.info("Clock of device {} was off by {} s; set to {} UTC", device.getId(), now - deviceEpoch, utc);
                eventService.publish(DeviceEventType.UPDATED, device.getId(), device.getName(), null,
                    "Relógio de " + device.getName() + " acertado pelo computador");
            } else {
                LOG.warn("Could not set the clock of device {}: {}", device.getId(), result.errorOutput());
            }
        } catch (DeviceConnectionException exception) {
            LOG.warn("Could not set the clock of device {}: {}", device.getId(), exception.getMessage());
        }
    }

    /** Same as {@link #refreshFacts(Long)}, but a failure only leaves the status updated. */
    public Device refreshFactsQuietly(Long id) {
        try {
            return refreshFacts(id);
        } catch (DeviceConnectionException exception) {
            LOG.info("Could not read facts of device {}: {}", id, exception.getMessage());
            return findById(id);
        }
    }

    public DeviceMetricsResponse metrics(Long id) {
        Device device = findById(id);
        CommandResult result = sshService.run(device, deviceScriptService.load("metrics"), List.of(), false, SCRIPT_TIMEOUT);
        KeyValueOutput output = KeyValueOutput.parse(result.output());
        return new DeviceMetricsResponse(LocalDateTime.now(), output.doubleValue("cpuPercent"), output.doubleValue("load1"),
            output.doubleValue("load5"), output.doubleValue("load15"), output.longValue("memoryTotalBytes"),
            output.longValue("memoryUsedBytes"), output.longValue("swapTotalBytes"), output.longValue("swapUsedBytes"),
            output.longValue("diskTotalBytes"), output.longValue("diskUsedBytes"), output.longValue("uptimeSeconds"),
            output.intValue("batteryPercent"), output.text("batteryStatus"), output.doubleValue("temperatureCelsius"),
            output.intValue("processCount"), output.longValue("networkReceivedBytes"), output.longValue("networkSentBytes"));
    }

    /** A device the panel can log into: registered, active and ready. */
    public Device requireReady(Long id) {
        Device device = findById(id);
        if (!device.isActive()) {
            throw new IllegalStateException("O dispositivo " + device.getName() + " está arquivado.");
        }
        if (device.getStatus() != DeviceStatus.READY) {
            throw new IllegalStateException("O painel ainda não entra em " + device.getName()
                + ". Vincule uma credencial que funcione em Configurações do dispositivo.");
        }
        return device;
    }
}
