package com.bancada.service;

import com.bancada.enums.CredentialAuthType;
import com.bancada.enums.MachineAction;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.OperationType;
import com.bancada.filter.MachineFilter;
import com.bancada.hyperv.CloudInit;
import com.bancada.hyperv.MachineKeys;
import com.bancada.models.Credential;
import com.bancada.models.Device;
import com.bancada.models.Machine;
import com.bancada.models.Operation;
import com.bancada.records.CommandResult;
import com.bancada.records.HyperVStatus;
import com.bancada.records.MachineStatusChangedEvent;
import com.bancada.records.VirtualMachineState;
import com.bancada.repository.MachineRepository;
import com.bancada.request.CredentialRequest;
import com.bancada.request.MachineRequest;
import com.bancada.response.DistributionResponse;
import com.bancada.response.MachineCreationResponse;
import com.bancada.response.MachineHostResponse;
import com.bancada.response.MachineLogsResponse;
import com.bancada.response.MachineResponse;
import com.bancada.response.MachineStatsResponse;
import com.bancada.specification.MachineSpecification;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Linux virtual machines on this PC (Hyper-V). A machine is created from the official cloud image
 * of its distribution, configured by cloud-init (user, password, keys, fixed address) and shows up
 * in the panel as a device of its own: once it answers, terminal, apps, files, backups and routes
 * work on it like on a phone or a board.
 */
@Service
public class MachineService {

    private static final Logger LOG = LoggerFactory.getLogger(MachineService.class);
    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CLOUD_INIT_TIMEOUT = Duration.ofMinutes(25);
    private static final long SSH_WAIT_MS = Duration.ofMinutes(10).toMillis();
    private static final int FIRST_ADDRESS = 10;
    private static final int LAST_ADDRESS = 254;
    private static final int LOG_LINES = 200;
    private static final List<String> DNS_SERVERS = List.of("1.1.1.1", "8.8.8.8");

    private final MachineRepository machineRepository;
    private final HyperVService hyperVService;
    private final MachineImageService machineImageService;
    private final HostDiskService hostDiskService;
    private final DeviceService deviceService;
    private final CredentialService credentialService;
    private final SshService sshService;
    private final OperationService operationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SecretCipherService secretCipherService;
    private final long reservedMemoryMb;

    public MachineService(MachineRepository machineRepository, HyperVService hyperVService, MachineImageService machineImageService,
                          HostDiskService hostDiskService, DeviceService deviceService, CredentialService credentialService,
                          SshService sshService, OperationService operationService, ApplicationEventPublisher applicationEventPublisher,
                          SecretCipherService secretCipherService, @Value("${bancada.vms.reserved-memory-mb}") long reservedMemoryMb) {
        this.machineRepository = machineRepository;
        this.hyperVService = hyperVService;
        this.machineImageService = machineImageService;
        this.hostDiskService = hostDiskService;
        this.deviceService = deviceService;
        this.credentialService = credentialService;
        this.sshService = sshService;
        this.operationService = operationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.secretCipherService = secretCipherService;
        this.reservedMemoryMb = reservedMemoryMb;
    }

    /** Creations cut by closing the panel end as failed (the next status sync corrects the rest). */
    @PostConstruct
    public void failInterruptedCreations() {
        for (Machine machine : machineRepository.findByStatus(MachineStatus.CREATING)) {
            machine.changeStatus(MachineStatus.FAILED);
            machineRepository.save(machine);
        }
    }

    @Transactional(readOnly = true)
    public Page<Machine> search(MachineFilter filter, Pageable pageable) {
        Specification<Machine> specification = Specification.where(MachineSpecification.search(filter.getSearch()))
            .and(MachineSpecification.statusIn(filter.getStatus()));
        return machineRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Machine findById(Long id) {
        return machineRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Máquina não encontrada para ID: " + id));
    }

    /** The machine behind a device, if the device is a virtual machine of this PC. */
    @Transactional(readOnly = true)
    public Machine findByDevice(Long deviceId) {
        return machineRepository.findByDeviceId(deviceId).orElse(null);
    }

    public List<DistributionResponse> distributions() {
        return Arrays.stream(MachineDistribution.values())
            .map(distribution -> new DistributionResponse(distribution, distribution.getDisplayName(), distribution.getVersions(), true))
            .toList();
    }

    /** Hyper-V, memory and processors of this PC, and how much the machines already took. */
    public MachineHostResponse host() {
        HyperVStatus status = hyperVService.status();
        List<Machine> machines = machineRepository.findByStatusNot(MachineStatus.REMOVED);
        long usedMemory = machines.stream().mapToLong(Machine::getMemoryMb).sum();
        long available = Math.max(0, status.memoryMb() - reservedMemoryMb - usedMemory);
        return new MachineHostResponse(status.installed(), status.permitted(), status.network(), status.ready(), status.message(),
            status.switchName(), status.network24(), status.cpus(), status.memoryMb(), reservedMemoryMb, usedMemory, available,
            machines.size(), hostDiskService.list());
    }

    public MachineCreationResponse create(MachineRequest request) {
        HyperVStatus status = hyperVService.requireReady();
        validateSystem(request.distribution(), request.version());
        if (machineRepository.existsByNameAndStatusNot(request.name(), MachineStatus.REMOVED)) {
            throw new DataIntegrityViolationException("Já existe uma máquina chamada \"" + request.name() + "\".");
        }
        requireCapacity(status, request.cpuCount(), request.memoryMb());
        long diskBytes = (long) request.diskGb() << 30;
        String drive = request.drive() == null || request.drive().isBlank()
            ? hostDiskService.roomiest(diskBytes).root()
            : hostDiskService.requireRoom(request.drive(), diskBytes).root();
        String address = allocateAddress();
        MachineKeys keys = MachineKeys.generate(request.name());
        Credential credential = credentialService.create(new CredentialRequest("Máquina " + request.name() + " (root)", "root",
            CredentialAuthType.PRIVATE_KEY, keys.panelPrivateKey(), null, false));
        Machine machine = machineRepository.save(new Machine(request, drive, address, macFor(address)));
        Device device = deviceService.registerVirtual(request.name(), address, keys.hostFingerprint(), "ssh-rsa", credential);
        machine.attachDevice(device, secretCipherService.encrypt(keys.hostPrivateKey()));
        Machine saved = machineRepository.save(machine);
        CloudInit seed = cloudInit(saved, request.password(), keys);
        Long machineId = saved.getId();
        Operation operation;
        try {
            operation = operationService.start(device, OperationType.MACHINE_CREATE, "Criar máquina " + saved.getName(), saved.getVmName(),
                operationId -> {
                    int exitCode = 1;
                    try {
                        exitCode = install(operationId, machineId, seed, true);
                        return exitCode;
                    } finally {
                        updateStatus(machineId, exitCode == 0 ? MachineStatus.RUNNING : MachineStatus.FAILED);
                    }
                });
        } catch (RuntimeException exception) {
            // nothing reached Hyper-V: the name, address and device must not stay taken
            forceStatus(machineId, MachineStatus.REMOVED);
            deviceService.changeActive(device.getId(), false);
            throw exception;
        }
        return new MachineCreationResponse(new MachineResponse(saved), operation.getId());
    }

    /**
     * Base image (downloaded once), disk, seed and VM; then waits for SSH and for cloud-init to
     * finish, and reads the facts of the new device. With {@code fresh} false the existing VM gets a
     * new disk and seed (reinstall).
     */
    public int install(Long operationId, Long machineId, CloudInit seed, boolean fresh) {
        Consumer<String> log = chunk -> operationService.appendOutput(operationId, chunk);
        Machine machine = findById(machineId);
        var baseDisk = machineImageService.ensureBaseDisk(machine.getDistribution(), machine.getVersion(), log);
        log.accept("== Gerando a configuração inicial (cloud-init) ==\n");
        hyperVService.buildSeedIso(seed.files(), machine.seedPath());
        int exitCode;
        if (fresh) {
            exitCode = hyperVService.prepareDisk(baseDisk, machine.diskPath(), machine.diskBytes(), log);
            if (exitCode != 0) {
                return exitCode;
            }
            exitCode = hyperVService.create(machine.getVmName(), machine.folder(), machine.diskPath(), machine.seedPath(),
                machine.getCpuCount(), machine.getMemoryMb(), machine.getMacAddress(), machine.isAutoStart(), log);
        } else {
            sshService.invalidate(machine.getDevice().getId());
            exitCode = hyperVService.reinstall(machine.getVmName(), baseDisk, machine.diskPath(), machine.diskBytes(), machine.seedPath(), log);
        }
        if (exitCode != 0) {
            return exitCode;
        }
        log.accept("== Esperando o sistema subir em " + machine.getIpAddress() + " ==\n");
        if (!waitForSsh(machine.getIpAddress())) {
            log.accept("A máquina não respondeu no SSH em " + SSH_WAIT_MS / 60_000 + " minutos. Veja o console dela no Gerenciador do Hyper-V.\n");
            return 1;
        }
        log.accept("== Terminando a configuração (cloud-init; a primeira vez instala as ferramentas do Hyper-V) ==\n");
        CommandResult cloudInit = sshService.run(machine.getDevice(), "cloud-init status --wait >/dev/null 2>&1; cloud-init status 2>&1 | head -1",
            List.of(), false, CLOUD_INIT_TIMEOUT);
        log.accept(cloudInit.output());
        deviceService.refreshFacts(machine.getDevice().getId());
        log.accept("== Máquina pronta: ssh " + machine.getUsername() + "@" + machine.getIpAddress() + " ==\n");
        return 0;
    }

    public Operation runAction(Long id, MachineAction action) {
        Machine machine = requireUsable(id);
        return operationService.start(machine.getDevice(), OperationType.MACHINE_ACTION, action.getDescription() + " " + machine.getName(),
            machine.getVmName(), operationId -> {
                switch (action) {
                    case START -> hyperVService.start(machine.getVmName());
                    case STOP -> hyperVService.stop(machine.getVmName(), false);
                    case RESTART -> hyperVService.restart(machine.getVmName());
                }
                operationService.appendOutput(operationId, "== " + action.getDescription() + ": concluído ==\n");
                updateStatus(id, action.getResultingStatus());
                return 0;
            });
    }

    /** Erases the VM and its folder; the device is archived and the record stays in the history. */
    public Operation remove(Long id) {
        Machine machine = findById(id);
        if (machine.getStatus() == MachineStatus.REMOVED) {
            throw new IllegalStateException("A máquina já foi removida.");
        }
        return operationService.start(machine.getDevice(), OperationType.MACHINE_REMOVE, "Remover máquina " + machine.getName(),
            machine.getVmName(), operationId -> {
                hyperVService.remove(machine.getVmName(), machine.folder());
                sshService.invalidate(machine.getDevice().getId());
                deviceService.changeActive(machine.getDevice().getId(), false);
                forceStatus(id, MachineStatus.REMOVED);
                operationService.appendOutput(operationId, "== Máquina " + machine.getName() + " removida (disco apagado) ==\n");
                return 0;
            });
    }

    /** Every 20 s: the real state of the VMs in Hyper-V fixes the stored one. */
    @Scheduled(initialDelay = 20_000, fixedDelay = 20_000)
    public void syncStatuses() {
        List<Machine> machines = machineRepository.findByStatusNot(MachineStatus.REMOVED);
        if (machines.isEmpty()) {
            return;
        }
        Map<String, VirtualMachineState> states = new HashMap<>();
        try {
            hyperVService.list().forEach(state -> states.put(state.name(), state));
        } catch (RuntimeException exception) {
            LOG.debug("Hyper-V not readable now: {}", exception.getMessage());
            return;
        }
        for (Machine machine : machines) {
            if (machine.getStatus() == MachineStatus.CREATING) {
                continue;
            }
            VirtualMachineState state = states.get(machine.getVmName());
            MachineStatus actual = state == null ? MachineStatus.FAILED
                : state.running() ? MachineStatus.RUNNING : state.off() ? MachineStatus.STOPPED : machine.getStatus();
            if (actual != machine.getStatus()) {
                updateStatus(machine.getId(), actual);
            }
        }
    }

    /** CPU and memory in use, as Hyper-V sees them. */
    public List<MachineStatsResponse> stats() {
        Map<String, VirtualMachineState> states = new HashMap<>();
        hyperVService.list().forEach(state -> states.put(state.name(), state));
        return machineRepository.findByStatusNot(MachineStatus.REMOVED).stream()
            .filter(machine -> states.containsKey(machine.getVmName()))
            .map(machine -> {
                VirtualMachineState state = states.get(machine.getVmName());
                double memoryPercent = machine.getMemoryMb() == 0 ? 0 : state.memoryMb() * 100.0 / machine.getMemoryMb();
                return new MachineStatsResponse(machine.getId(), (double) state.cpuPercent(),
                    state.memoryMb() + " MB / " + machine.getMemoryMb() + " MB", memoryPercent, null);
            })
            .toList();
    }

    /** Last lines of the system journal of the machine. */
    public MachineLogsResponse logs(Long id) {
        Machine machine = requireRunning(id);
        CommandResult result = sshService.run(machine.getDevice(),
            "journalctl -n " + LOG_LINES + " --no-pager 2>/dev/null || tail -n " + LOG_LINES + " /var/log/syslog /var/log/messages 2>/dev/null",
            List.of(), false, QUERY_TIMEOUT);
        return new MachineLogsResponse(machine.getId(), result.output());
    }

    /** New password for the machine user (SSH and sudo). */
    public void changeUserPassword(Long id, String password) {
        Machine machine = requireRunning(id);
        CommandResult result = sshService.run(machine.getDevice(),
            "printf '%s:%s\\n' " + SshService.quote(machine.getUsername()) + " " + SshService.quote(password) + " | chpasswd\n",
            List.of(), false, QUERY_TIMEOUT);
        if (!result.succeeded()) {
            throw new IllegalStateException("A senha não foi trocada: " + result.errorOutput().trim());
        }
    }

    /** The keys the machine was created with: a reinstall keeps its host key and the panel access. */
    public MachineKeys keysOf(Machine machine) {
        Credential credential = machine.getDevice().getCredential();
        String hostKey = secretCipherService.decrypt(machine.getEncryptedHostKey());
        String panelKey = credential == null ? null : secretCipherService.decrypt(credential.getEncryptedSecret());
        if (hostKey == null || panelKey == null) {
            throw new IllegalStateException("As chaves da máquina " + machine.getName() + " não estão mais guardadas.");
        }
        return MachineKeys.restore(hostKey, panelKey, machine.getName());
    }

    /** Seed for a machine: a new instance id makes cloud-init run again on a reinstall. */
    public CloudInit cloudInit(Machine machine, String password, MachineKeys keys) {
        return new CloudInit(machine.getName(), machine.getVmName() + "-" + UUID.randomUUID().toString().substring(0, 8),
            machine.getUsername(), password, machine.getDistribution().getAdminGroup(), keys.panelPublicKey(), keys.hostPrivateKey(),
            keys.hostPublicKey(), machine.getMacAddress(), machine.getIpAddress(), 24, hyperVService.gateway(), DNS_SERVERS);
    }

    /** Applies the status when the lifecycle allows it (a removed machine stays removed). */
    public void updateStatus(Long id, MachineStatus target) {
        Machine machine = findById(id);
        if (machine.getStatus() != target && machine.getStatus().canTransitionTo(target)) {
            machine.changeStatus(target);
            machineRepository.save(machine);
            applicationEventPublisher.publishEvent(new MachineStatusChangedEvent(id, target));
        }
    }

    /** Marks the machine as being prepared (reinstall, restore) and returns the status it had. */
    @Transactional
    public MachineStatus startPreparation(Long id) {
        Machine machine = findById(id);
        MachineStatus previous = machine.getStatus();
        if (previous == MachineStatus.REMOVED || previous == MachineStatus.CREATING) {
            throw new IllegalStateException("A máquina " + machine.getName() + " está " + previous.getDescription().toLowerCase() + ".");
        }
        machine.changeStatus(MachineStatus.CREATING);
        machineRepository.save(machine);
        return previous;
    }

    @Transactional
    public Machine switchSystem(Long id, MachineDistribution distribution, String version) {
        Machine machine = findById(id);
        machine.switchSystem(distribution, version);
        return machineRepository.save(machine);
    }

    public void validateSystem(MachineDistribution distribution, String version) {
        if (!distribution.getVersions().contains(version)) {
            throw new IllegalArgumentException("Versão não oferecida: " + distribution.getDisplayName() + " " + version);
        }
    }

    /** Refuses a machine this PC cannot hold: processors and memory left after Windows and the other machines. */
    public void requireCapacity(HyperVStatus status, int cpus, int memoryMb) {
        if (cpus > status.cpus()) {
            throw new IllegalArgumentException("Este PC tem " + status.cpus() + " processadores lógicos; a máquina não pode ter mais.");
        }
        long used = machineRepository.findByStatusNot(MachineStatus.REMOVED).stream().mapToLong(Machine::getMemoryMb).sum();
        long available = status.memoryMb() - reservedMemoryMb - used;
        if (memoryMb > available) {
            throw new IllegalStateException("Não há memória para mais " + memoryMb + " MB: o PC tem " + status.memoryMb() + " MB, "
                + reservedMemoryMb + " ficam para o Windows e as máquinas já usam " + used + " MB (sobram " + Math.max(0, available) + " MB).");
        }
    }

    private Machine requireUsable(Long id) {
        Machine machine = findById(id);
        if (machine.getStatus() == MachineStatus.REMOVED || machine.getStatus() == MachineStatus.CREATING) {
            throw new IllegalStateException("A máquina " + machine.getName() + " está " + machine.getStatus().getDescription().toLowerCase() + ".");
        }
        return machine;
    }

    private Machine requireRunning(Long id) {
        Machine machine = findById(id);
        if (machine.getStatus() != MachineStatus.RUNNING) {
            throw new IllegalStateException("Ligue a máquina " + machine.getName() + " antes.");
        }
        return machine;
    }

    /** First free address of the machine network (from .10), never reused while its machine exists. */
    private String allocateAddress() {
        Set<String> taken = new HashSet<>();
        machineRepository.findByStatusNot(MachineStatus.REMOVED).forEach(machine -> taken.add(machine.getIpAddress()));
        for (int last = FIRST_ADDRESS; last <= LAST_ADDRESS; last++) {
            String address = hyperVService.network() + "." + last;
            if (!taken.contains(address)) {
                return address;
            }
        }
        throw new IllegalStateException("A rede das máquinas está cheia (" + (LAST_ADDRESS - FIRST_ADDRESS + 1) + " endereços).");
    }

    /** Hyper-V MAC range (00:15:5D) with the address in the last bytes: unique on this PC. */
    static String macFor(String address) {
        String[] octets = address.split("\\.");
        return String.format(Locale.ROOT, "00:15:5D:%02X:%02X:%02X", Integer.parseInt(octets[1]), Integer.parseInt(octets[2]),
            Integer.parseInt(octets[3]));
    }

    private static boolean waitForSsh(String address) {
        long deadline = System.currentTimeMillis() + SSH_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address, 22), 2_000);
                return true;
            } catch (IOException exception) {
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /** Removal always ends as REMOVED, whatever state the VM was in. */
    private void forceStatus(Long id, MachineStatus target) {
        Machine machine = findById(id);
        if (machine.getStatus() != target) {
            if (machine.getStatus() == MachineStatus.CREATING) {
                machine.changeStatus(MachineStatus.FAILED);
            }
            machine.changeStatus(target);
            machineRepository.save(machine);
            applicationEventPublisher.publishEvent(new MachineStatusChangedEvent(id, target));
        }
    }
}
