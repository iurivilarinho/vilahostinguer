package com.bancada.service;

import com.bancada.enums.ConnectionType;
import com.bancada.enums.DeviceEventType;
import com.bancada.models.AppSettings;
import com.bancada.models.Device;
import com.bancada.records.DiscoveryCandidate;
import com.bancada.records.HostKeyProbe;
import com.bancada.repository.DeviceRepository;
import com.bancada.response.ScanStatusResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Finds devices plugged into this computer (or reachable on the network) and keeps their online
 * status current.
 *
 * <p>A phone on the USB cable shows up as a network adapter here. When Windows gets no DHCP on it,
 * the adapter takes a link-local 169.254.x.y address, and the phone side sits on 169.254.1.1 by
 * convention. Besides that fixed guess, every neighbor the ARP table shows on those adapters is
 * probed. An address only becomes a device after its SSH host key is read, because the key, not the
 * address, tells one device from another.
 */
@Service
public class DiscoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryService.class);

    private static final int SSH_PORT = 22;
    private static final String USB_DEFAULT_PEER = "169.254.1.1";
    private static final int TCP_TIMEOUT_MS = 800;
    private static final Duration FINGERPRINT_TTL = Duration.ofSeconds(60);
    private static final Duration LAST_SEEN_WRITE_INTERVAL = Duration.ofSeconds(60);
    private static final Pattern USB_ADAPTER = Pattern.compile("(?i).*(ncm|rndis|remote ndis|usb|cdc ethernet|gadget).*");
    private static final Pattern ARP_INTERFACE = Pattern.compile("^\\S+:\\s+(\\d{1,3}(?:\\.\\d{1,3}){3})\\s+---.*");
    private static final Pattern ARP_ENTRY = Pattern.compile("^\\s+(\\d{1,3}(?:\\.\\d{1,3}){3})\\s+([0-9a-fA-F]{2}(?:[-:][0-9a-fA-F]{2}){5})\\s+.*");

    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final CredentialService credentialService;
    private final SettingsService settingsService;
    private final SshService sshService;
    private final EventService eventService;
    private final Executor operationExecutor;
    private final ExecutorService probePool = Executors.newFixedThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "discovery-probe");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, CachedProbe> fingerprintCache = new ConcurrentHashMap<>();
    private final boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");

    private volatile long lastRunMillis;
    private volatile LocalDateTime lastScanAt;
    private volatile List<String> lastUsbInterfaces = List.of();
    private volatile List<String> lastProbedHosts = List.of();
    private volatile List<String> lastReachableHosts = List.of();

    public DiscoveryService(DeviceRepository deviceRepository, DeviceService deviceService, CredentialService credentialService,
                            SettingsService settingsService, SshService sshService, EventService eventService,
                            @Qualifier("operationExecutor") Executor operationExecutor) {
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.credentialService = credentialService;
        this.settingsService = settingsService;
        this.sshService = sshService;
        this.eventService = eventService;
        this.operationExecutor = operationExecutor;
    }

    @Scheduled(initialDelay = 3_000, fixedDelay = 1_000)
    public void tick() {
        AppSettings settings = settingsService.get();
        if (!settings.isScanEnabled()) {
            return;
        }
        if (System.currentTimeMillis() - lastRunMillis < settings.getScanIntervalSeconds() * 1_000L) {
            return;
        }
        scanNow();
    }

    public synchronized ScanStatusResponse scanNow() {
        lastRunMillis = System.currentTimeMillis();
        AppSettings settings = settingsService.get();
        List<String> usbInterfaces = new ArrayList<>();
        Map<String, DiscoveryCandidate> candidates = new LinkedHashMap<>();
        collectUsbCandidates(usbInterfaces, candidates);
        for (String extra : settings.extraHostList()) {
            DiscoveryCandidate candidate = parseExtraHost(extra);
            candidates.putIfAbsent(key(candidate.host(), candidate.port()), candidate);
        }
        List<Device> registered = deviceRepository.findByActiveTrue();
        for (Device device : registered) {
            candidates.putIfAbsent(key(device.getHost(), device.getPort()),
                new DiscoveryCandidate(device.getHost(), device.getPort(), null, null));
        }

        List<DiscoveryCandidate> reachable = probeReachable(new ArrayList<>(candidates.values()));
        Map<DiscoveryCandidate, HostKeyProbe> identified = identify(reachable);

        Set<Long> seenDeviceIds = new HashSet<>();
        for (Map.Entry<DiscoveryCandidate, HostKeyProbe> entry : identified.entrySet()) {
            try {
                Device device = handleFound(entry.getKey(), entry.getValue(), settings);
                seenDeviceIds.add(device.getId());
            } catch (RuntimeException exception) {
                LOG.warn("Could not register {}: {}", entry.getKey().host(), exception.getMessage());
            }
        }
        markMissingOffline(seenDeviceIds);

        lastScanAt = LocalDateTime.now();
        lastUsbInterfaces = List.copyOf(usbInterfaces);
        lastProbedHosts = candidates.values().stream().map(candidate -> key(candidate.host(), candidate.port())).toList();
        lastReachableHosts = reachable.stream().map(candidate -> key(candidate.host(), candidate.port())).toList();
        return status();
    }

    public ScanStatusResponse status() {
        return new ScanStatusResponse(settingsService.get().isScanEnabled(), lastScanAt, lastUsbInterfaces,
            lastProbedHosts, lastReachableHosts);
    }

    private void collectUsbCandidates(List<String> usbInterfaces, Map<String, DiscoveryCandidate> candidates) {
        Map<String, List<String>> arpTable = readArpTable();
        List<NetworkInterface> interfaces;
        try {
            interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException exception) {
            LOG.warn("Could not list network interfaces: {}", exception.getMessage());
            return;
        }
        for (NetworkInterface networkInterface : interfaces) {
            try {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
            } catch (SocketException exception) {
                continue;
            }
            String displayName = Optional.ofNullable(networkInterface.getDisplayName()).orElse(networkInterface.getName());
            boolean usbAdapter = USB_ADAPTER.matcher(displayName).matches();
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress address = interfaceAddress.getAddress();
                if (!(address instanceof Inet4Address)) {
                    continue;
                }
                boolean linkLocal = address.isLinkLocalAddress();
                if (!linkLocal && !usbAdapter) {
                    continue;
                }
                String localIp = address.getHostAddress();
                usbInterfaces.add(displayName + " (" + localIp + ")");
                String peer = linkLocal ? USB_DEFAULT_PEER : firstHostOf(address, interfaceAddress.getNetworkPrefixLength());
                if (peer != null && !peer.equals(localIp)) {
                    candidates.putIfAbsent(key(peer, SSH_PORT), new DiscoveryCandidate(peer, SSH_PORT, ConnectionType.USB, displayName));
                }
                List<String> neighbors = new ArrayList<>(arpTable.getOrDefault(localIp, List.of()));
                neighbors.addAll(arpTable.getOrDefault(networkInterface.getName(), List.of()));
                for (String neighbor : neighbors) {
                    if (!neighbor.equals(localIp) && isUnicast(neighbor)) {
                        candidates.putIfAbsent(key(neighbor, SSH_PORT),
                            new DiscoveryCandidate(neighbor, SSH_PORT, ConnectionType.USB, displayName));
                    }
                }
            }
        }
    }

    /**
     * Neighbors by interface. Windows ({@code arp -a}) groups them under the local IP of the adapter;
     * Linux ({@code /proc/net/arp}) names the device. Both keys are looked up by the caller.
     */
    private Map<String, List<String>> readArpTable() {
        Map<String, List<String>> table = new HashMap<>();
        try {
            if (windows) {
                Process process = new ProcessBuilder("arp", "-a").redirectErrorStream(true).start();
                String output;
                try (InputStream input = process.getInputStream()) {
                    output = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                }
                process.waitFor(3, TimeUnit.SECONDS);
                String currentInterface = null;
                for (String line : output.split("\r?\n")) {
                    Matcher header = ARP_INTERFACE.matcher(line);
                    if (header.matches()) {
                        currentInterface = header.group(1);
                        continue;
                    }
                    Matcher entry = ARP_ENTRY.matcher(line);
                    if (currentInterface != null && entry.matches()) {
                        table.computeIfAbsent(currentInterface, ignored -> new ArrayList<>()).add(entry.group(1));
                    }
                }
            } else {
                Path arp = Path.of("/proc/net/arp");
                if (Files.isReadable(arp)) {
                    for (String line : Files.readAllLines(arp)) {
                        String[] columns = line.trim().split("\\s+");
                        if (columns.length >= 6 && columns[0].matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            table.computeIfAbsent(columns[5], ignored -> new ArrayList<>()).add(columns[0]);
                        }
                    }
                }
            }
        } catch (IOException exception) {
            LOG.debug("Could not read the ARP table: {}", exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        return table;
    }

    private List<DiscoveryCandidate> probeReachable(List<DiscoveryCandidate> candidates) {
        List<CompletableFuture<Optional<DiscoveryCandidate>>> probes = candidates.stream()
            .map(candidate -> CompletableFuture.supplyAsync(
                () -> portOpen(candidate.host(), candidate.port()) ? Optional.of(candidate) : Optional.<DiscoveryCandidate>empty(),
                probePool))
            .toList();
        return probes.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList();
    }

    private Map<DiscoveryCandidate, HostKeyProbe> identify(List<DiscoveryCandidate> reachable) {
        Map<DiscoveryCandidate, CompletableFuture<HostKeyProbe>> pending = new LinkedHashMap<>();
        for (DiscoveryCandidate candidate : reachable) {
            String cacheKey = key(candidate.host(), candidate.port());
            CachedProbe cached = fingerprintCache.get(cacheKey);
            if (cached != null && cached.readAt().plus(FINGERPRINT_TTL).isAfter(LocalDateTime.now())) {
                pending.put(candidate, CompletableFuture.completedFuture(cached.probe()));
                continue;
            }
            pending.put(candidate, CompletableFuture.supplyAsync(() -> {
                HostKeyProbe probe = sshService.probeHostKey(candidate.host(), candidate.port());
                if (probe != null) {
                    fingerprintCache.put(cacheKey, new CachedProbe(probe, LocalDateTime.now()));
                }
                return probe;
            }, probePool));
        }
        Map<DiscoveryCandidate, HostKeyProbe> identified = new LinkedHashMap<>();
        pending.forEach((candidate, future) -> {
            HostKeyProbe probe = future.join();
            if (probe != null) {
                identified.put(candidate, probe);
            }
        });
        return identified;
    }

    private Device handleFound(DiscoveryCandidate candidate, HostKeyProbe probe, AppSettings settings) {
        Optional<Device> existing = deviceRepository.findByHostKeyFingerprint(probe.fingerprint());
        if (existing.isEmpty()) {
            ConnectionType connectionType = candidate.connectionType() == null ? ConnectionType.NETWORK : candidate.connectionType();
            Device created = deviceRepository.save(new Device(candidate.host(), candidate.port(), probe.fingerprint(),
                probe.keyType(), connectionType, candidate.interfaceName()));
            LOG.info("New device at {} ({})", candidate.host(), probe.fingerprint());
            eventService.publish(DeviceEventType.DISCOVERED, created.getId(), created.getName(), null,
                "Novo dispositivo conectado em " + candidate.host());
            if (settings.isAutoSetup()) {
                credentialService.findDefault().ifPresent(credential -> {
                    created.setCredential(credential);
                    deviceRepository.save(created);
                    operationExecutor.execute(() -> deviceService.refreshFactsQuietly(created.getId()));
                });
            }
            return created;
        }

        Device device = existing.get();
        if (!device.isActive()) {
            return device;
        }
        boolean cameOnline = !device.isOnline();
        boolean addressChanged = !device.getHost().equals(candidate.host()) || device.getPort() != candidate.port();
        boolean stale = device.getLastSeenAt() == null
            || device.getLastSeenAt().plus(LAST_SEEN_WRITE_INTERVAL).isBefore(LocalDateTime.now());
        if (cameOnline || addressChanged || stale) {
            ConnectionType connectionType = candidate.connectionType() == null ? device.getConnectionType() : candidate.connectionType();
            String interfaceName = candidate.connectionType() == null ? device.getInterfaceName() : candidate.interfaceName();
            device.markSeen(candidate.host(), connectionType, interfaceName);
            deviceRepository.save(device);
        }
        if (addressChanged) {
            sshService.invalidate(device.getId());
        }
        if (cameOnline) {
            eventService.publish(DeviceEventType.ONLINE, device.getId(), device.getName(), null,
                device.getName() + " conectado em " + device.getHost());
            if (device.getCredential() != null) {
                operationExecutor.execute(() -> deviceService.refreshFactsQuietly(device.getId()));
            }
        }
        return device;
    }

    private void markMissingOffline(Set<Long> seenDeviceIds) {
        for (Device device : deviceRepository.findByActiveTrue()) {
            if (device.isOnline() && !seenDeviceIds.contains(device.getId())) {
                device.setOnline(false);
                deviceRepository.save(device);
                sshService.invalidate(device.getId());
                eventService.publish(DeviceEventType.OFFLINE, device.getId(), device.getName(), null,
                    device.getName() + " desconectado");
            }
        }
    }

    private static boolean portOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TCP_TIMEOUT_MS);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static String firstHostOf(InetAddress address, short prefixLength) {
        if (prefixLength <= 0 || prefixLength >= 31) {
            return null;
        }
        byte[] bytes = address.getAddress();
        int value = ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
        int mask = -1 << (32 - prefixLength);
        int first = (value & mask) + 1;
        return ((first >>> 24) & 0xff) + "." + ((first >>> 16) & 0xff) + "." + ((first >>> 8) & 0xff) + "." + (first & 0xff);
    }

    private static boolean isUnicast(String ip) {
        String[] octets = ip.split("\\.");
        int first = Integer.parseInt(octets[0]);
        return !octets[3].equals("255") && !octets[3].equals("0") && first < 224;
    }

    private static DiscoveryCandidate parseExtraHost(String value) {
        int separator = value.lastIndexOf(':');
        if (separator > 0 && value.indexOf(':') == separator) {
            try {
                return new DiscoveryCandidate(value.substring(0, separator), Integer.parseInt(value.substring(separator + 1)),
                    ConnectionType.NETWORK, null);
            } catch (NumberFormatException exception) {
                return new DiscoveryCandidate(value, SSH_PORT, ConnectionType.NETWORK, null);
            }
        }
        return new DiscoveryCandidate(value, SSH_PORT, ConnectionType.NETWORK, null);
    }

    private static String key(String host, int port) {
        return host + ":" + port;
    }

    /** Last host key read from an address; avoids a key exchange on every scan. */
    private record CachedProbe(HostKeyProbe probe, LocalDateTime readAt) {
    }
}
