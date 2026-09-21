package com.bancada.service;

import com.bancada.enums.DeviceStatus;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.OperationType;
import com.bancada.enums.PackageManager;
import com.bancada.enums.VolumeStatus;
import com.bancada.filter.VolumeFilter;
import com.bancada.models.Device;
import com.bancada.models.Machine;
import com.bancada.models.Operation;
import com.bancada.models.Volume;
import com.bancada.nbd.NbdServer;
import com.bancada.nbd.VolumeImage;
import com.bancada.records.CommandResult;
import com.bancada.records.MachineStatusChangedEvent;
import com.bancada.repository.VolumeRepository;
import com.bancada.request.VolumeAttachRequest;
import com.bancada.request.VolumeRequest;
import com.bancada.specification.VolumeSpecification;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disks of this PC lent to the devices. The file lives here; the device attaches it over the
 * network (NBD) and mounts it; a virtual machine of this PC is a device like any other. A device that reboots gets
 * its disks back as soon as it answers again; when this PC restarts, {@code nbd-client -persist}
 * on the device reconnects by itself.
 */
@Service
public class VolumeService {

    private static final Logger LOG = LoggerFactory.getLogger(VolumeService.class);
    private static final long GIGABYTE = 1L << 30;
    private static final Duration RECONNECT_TIMEOUT = Duration.ofMinutes(3);
    private static final String DEVICE_MOUNT_ROOT = "/mnt/";
    private static final List<String> MOUNT_ROOTS = List.of("/mnt/", "/media/", "/srv/", "/home/", "/opt/", "/data/");
    private static final Set<String> SYSTEM_FOLDERS = Set.of("/", "/bin", "/boot", "/dev", "/etc", "/lib", "/lib64", "/proc",
        "/root", "/run", "/sbin", "/sys", "/tmp", "/usr", "/var", "/home", "/srv", "/opt", "/mnt", "/media");
    private static final List<String> SYSTEM_PREFIXES = List.of("/bin/", "/boot/", "/dev/", "/etc/", "/lib/", "/lib64/", "/proc/",
        "/run/", "/sbin/", "/sys/");

    private final VolumeRepository volumeRepository;
    private final HostDiskService hostDiskService;
    private final DeviceService deviceService;
    private final MachineService machineService;
    private final SshService sshService;
    private final DeviceScriptService deviceScriptService;
    private final OperationService operationService;
    private final Executor operationExecutor;
    private final int nbdPort;
    private final SecureRandom random = new SecureRandom();
    private final Set<Long> reconnecting = ConcurrentHashMap.newKeySet();
    private volatile NbdServer nbdServer;
    private volatile String nbdError;

    public VolumeService(VolumeRepository volumeRepository, HostDiskService hostDiskService, DeviceService deviceService,
                         MachineService machineService, SshService sshService, DeviceScriptService deviceScriptService,
                         OperationService operationService, @Qualifier("operationExecutor") Executor operationExecutor,
                         @Value("${bancada.nbd.port}") int nbdPort) {
        this.volumeRepository = volumeRepository;
        this.hostDiskService = hostDiskService;
        this.deviceService = deviceService;
        this.machineService = machineService;
        this.sshService = sshService;
        this.deviceScriptService = deviceScriptService;
        this.operationService = operationService;
        this.operationExecutor = operationExecutor;
        this.nbdPort = nbdPort;
    }

    /** Nothing is connected when the panel starts; operations cut in half end as failed. */
    @PostConstruct
    public void closeInterrupted() {
        for (Volume volume : volumeRepository.findByStatusIn(List.of(VolumeStatus.ATTACHED, VolumeStatus.ATTACHING,
            VolumeStatus.DETACHING))) {
            if (volume.getStatus() == VolumeStatus.ATTACHED) {
                volume.changeStatus(VolumeStatus.WAITING, "Esperando o dispositivo reconectar ao painel.");
            } else {
                volume.changeStatus(VolumeStatus.FAILED, "O painel foi fechado durante a operação. Tente de novo.");
            }
            volumeRepository.save(volume);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        try {
            nbdServer = new NbdServer(new InetSocketAddress(nbdPort), this::resolveExport, new NbdServer.ConnectionListener() {
                @Override
                public void connected(Long volumeId, String client) {
                    onConnected(volumeId);
                }

                @Override
                public void disconnected(Long volumeId, String reason) {
                    onDisconnected(volumeId, reason);
                }
            });
            nbdError = null;
        } catch (IOException exception) {
            nbdError = "A porta " + nbdPort + " dos discos do PC não abriu: " + exception.getMessage();
            LOG.warn(nbdError);
        }
    }

    @PreDestroy
    public void stopServer() {
        if (nbdServer != null) {
            nbdServer.close();
        }
    }

    @Transactional(readOnly = true)
    public Page<Volume> search(VolumeFilter filter, Pageable pageable) {
        Specification<Volume> specification = Specification.where(VolumeSpecification.search(filter.getSearch()))
            .and(VolumeSpecification.statusIn(filter.getStatus()))
            .and(VolumeSpecification.device(filter.getDeviceId()))
            .and(VolumeSpecification.machine(filter.getMachineId()))
            .and(VolumeSpecification.drive(filter.getDrive()));
        return volumeRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Volume findById(Long id) {
        return volumeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Disco não encontrado para ID: " + id));
    }

    public long writtenBytes(Volume volume) {
        return VolumeImage.writtenBytes(Paths.get(volume.getFilePath()), volume.getSizeBytes());
    }

    /** Creates the sparse file; the space counts as reserved from now on. */
    public Volume create(VolumeRequest request) {
        if (volumeRepository.existsByNameAndStatusNot(request.name(), VolumeStatus.DELETED)) {
            throw new DataIntegrityViolationException("Já existe um disco chamado \"" + request.name() + "\".");
        }
        long size = request.sizeGb() * GIGABYTE;
        String drive = hostDiskService.requireRoom(request.drive(), size).root();
        Path file = HostDiskService.folderOf(drive).resolve(request.name() + ".img");
        if (Files.exists(file)) {
            throw new IllegalStateException("Já existe o arquivo " + file + ". Apague-o ou escolha outro nome.");
        }
        try {
            VolumeImage.create(file, size);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível criar o arquivo do disco em " + drive + ": " + exception.getMessage(), exception);
        }
        String exportName = HexFormat.of().formatHex(randomBytes());
        try {
            return volumeRepository.save(new Volume(request.name(), drive, file.toString(), size, exportName));
        } catch (RuntimeException exception) {
            deleteQuietly(file);
            throw exception;
        }
    }

    /**
     * Gives the disk to a device, mounted at a folder. A machine is a device of its own (a virtual
     * machine of this PC): the disk is mounted inside it, at the folder asked for.
     */
    public Operation attach(Long id, VolumeAttachRequest request) {
        requireServer();
        Volume volume = findById(id);
        if (volume.getStatus() != VolumeStatus.AVAILABLE && volume.getStatus() != VolumeStatus.FAILED) {
            throw new IllegalStateException("O disco " + volume.getName() + " está " + volume.getStatus().getDescription().toLowerCase()
                + ". Desconecte antes de entregar a outro lugar.");
        }
        Machine machine = request.machineId() == null ? null : machineService.findById(request.machineId());
        if (machine != null && machine.getStatus() != MachineStatus.RUNNING) {
            throw new IllegalStateException("Ligue a máquina " + machine.getName() + " antes de conectar o disco.");
        }
        Long deviceId = machine != null ? machine.getDevice().getId() : request.deviceId();
        if (deviceId == null) {
            throw new IllegalArgumentException("Escolha o dispositivo ou a máquina.");
        }
        if (volume.getDevice() != null && !volume.getDevice().getId().equals(deviceId)) {
            throw new IllegalStateException("O disco ainda está registrado em " + volume.getDevice().getName() + ". Desconecte antes.");
        }
        Device device = deviceService.requireReady(deviceId);
        String mountPath;
        if (machine != null) {
            mountPath = blankToNull(request.containerPath()) != null ? blankToNull(request.containerPath()) : blankToNull(request.mountPath());
            if (mountPath == null) {
                mountPath = DEVICE_MOUNT_ROOT + volume.getName();
            }
            validateContainerPath(mountPath);
        } else {
            mountPath = blankToNull(request.mountPath()) == null ? DEVICE_MOUNT_ROOT + volume.getName() : blankToNull(request.mountPath());
            validateMountPath(mountPath);
        }
        if (volumeRepository.existsByDeviceIdAndMountPathAndIdNot(deviceId, mountPath, id)) {
            throw new IllegalArgumentException("Outro disco já usa a pasta " + mountPath + " neste dispositivo.");
        }
        String pcAddress = localAddressFor(device.getHost());
        assign(id, device, mountPath, machine, machine == null ? null : mountPath);
        String title = "Conectar o disco " + volume.getName() + (machine == null ? " em " + mountPath : " à máquina " + machine.getName());
        String script = attachScript(volume, device, pcAddress, mountPath);
        String owner = machine == null ? null : machine.getUsername();
        String mounted = mountPath;
        try {
            return operationService.start(device, OperationType.VOLUME_ATTACH, title, volume.getName(), operationId -> {
                StringBuilder output = new StringBuilder();
                int exitCode = 1;
                try {
                    exitCode = sshService.stream(device, script, true, chunk -> {
                            output.append(chunk);
                            operationService.appendOutput(operationId, chunk);
                        },
                        channel -> operationService.registerChannel(operationId, channel));
                    if (exitCode == 0 && output.indexOf("FORMATTED=1") >= 0) {
                        markFormatted(id);
                        if (owner != null) {
                            // a fresh disk of a machine belongs to its user, not to root
                            sshService.run(device, "chown " + SshService.quote(owner) + ": " + SshService.quote(mounted), List.of(), true,
                                RECONNECT_TIMEOUT);
                        }
                    }
                    return exitCode;
                } catch (RuntimeException exception) {
                    // the device did not answer: the reason goes to the disk as well as to the activity
                    output.append('\n').append(exception.getMessage()).append('\n');
                    throw exception;
                } finally {
                    finishAttach(id, exitCode, output);
                }
            });
        } catch (RuntimeException exception) {
            changeStatus(id, VolumeStatus.FAILED, exception.getMessage());
            throw exception;
        }
    }

    /** Unmounts on the device and disconnects. */
    public Operation detach(Long id) {
        Volume volume = requireHeld(id);
        Device device = deviceService.requireReady(volume.getDevice().getId());
        changeStatus(id, VolumeStatus.DETACHING, null);
        String script = "volume_id=" + volume.getId() + "\nmount_path=" + SshService.quote(volume.getMountPath()) + "\n"
            + deviceScriptService.load("volume-detach");
        try {
            return operationService.start(device, OperationType.VOLUME_DETACH, "Desconectar o disco " + volume.getName(),
                volume.getName(), operationId -> {
                    int exitCode = 1;
                    String failure = "Não desconectou. Veja a atividade para o motivo.";
                    try {
                        exitCode = sshService.stream(device, script, true, chunk -> operationService.appendOutput(operationId, chunk),
                            channel -> operationService.registerChannel(operationId, channel));
                        return exitCode;
                    } catch (RuntimeException exception) {
                        exitCode = 1;
                        failure = exception.getMessage();
                        throw exception;
                    } finally {
                        finishDetach(id, exitCode, failure);
                    }
                });
        } catch (RuntimeException exception) {
            changeStatus(id, VolumeStatus.FAILED, exception.getMessage());
            throw exception;
        }
    }

    /** Erases the file on the PC. Only a disk that no device holds can go. */
    @Transactional
    public Volume delete(Long id, String confirmation) {
        Volume volume = findById(id);
        if (!volume.getName().equals(confirmation == null ? null : confirmation.trim())) {
            throw new IllegalArgumentException("A confirmação não confere com o nome do disco.");
        }
        if (volume.getDevice() != null) {
            throw new IllegalStateException("Desconecte o disco de " + volume.getDevice().getName() + " antes de apagar.");
        }
        deleteQuietly(Paths.get(volume.getFilePath()));
        volume.changeStatus(VolumeStatus.DELETED, null);
        return volumeRepository.save(volume);
    }

    public String serverError() {
        return nbdServer == null && nbdError == null ? "O servidor de discos ainda não subiu." : nbdError;
    }

    public int port() {
        return nbdPort;
    }

    /** A removed machine lets go of its disk; the disk stays mounted on the device. */
    @EventListener
    public void onMachineStatusChanged(MachineStatusChangedEvent event) {
        if (event.status() != MachineStatus.REMOVED) {
            return;
        }
        for (Volume volume : volumeRepository.findByMachineId(event.machineId())) {
            volume.forgetMachine();
            volumeRepository.save(volume);
        }
    }

    /**
     * Every minute: disks whose device came back (a reboot drops every NBD attachment) are attached
     * and mounted again.
     */
    @Scheduled(initialDelay = 90_000, fixedDelay = 60_000)
    public void reconnectWaiting() {
        if (nbdServer == null) {
            return;
        }
        for (Volume volume : volumeRepository.findByStatusIn(List.of(VolumeStatus.WAITING))) {
            Device device = volume.getDevice();
            if (device == null || !device.isActive() || !device.isOnline() || device.getStatus() != DeviceStatus.READY
                || nbdServer.isConnected(volume.getId()) || !reconnecting.add(volume.getId())) {
                continue;
            }
            operationExecutor.execute(() -> {
                try {
                    reconnect(volume.getId());
                } finally {
                    reconnecting.remove(volume.getId());
                }
            });
        }
    }

    private void reconnect(Long id) {
        Volume volume = findById(id);
        Device device = volume.getDevice();
        try {
            String script = attachScript(volume, device, localAddressFor(device.getHost()), volume.getMountPath());
            CommandResult result = sshService.run(device, script, List.of(), true, RECONNECT_TIMEOUT);
            if (!result.succeeded()) {
                changeMessage(id, "Não reconectou: " + lastLine(result.output() + "\n" + result.errorOutput()));
                return;
            }
        } catch (RuntimeException exception) {
            changeMessage(id, "Não reconectou: " + exception.getMessage());
        }
    }

    /** Only the device the disk was given to gets it, and only by its secret name. */
    private NbdServer.Export resolveExport(String name, InetAddress client) {
        Volume volume = volumeRepository.findByExportNameAndStatusNot(name, VolumeStatus.DELETED).orElse(null);
        if (volume == null || volume.getDevice() == null) {
            return null;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(volume.getDevice().getHost())) {
                if (address.equals(client)) {
                    return new NbdServer.Export(volume.getId(), Paths.get(volume.getFilePath()));
                }
            }
        } catch (UnknownHostException exception) {
            LOG.warn("Device address of disk {} does not resolve: {}", volume.getId(), exception.getMessage());
        }
        LOG.warn("Disk {} asked from {}, which is not its device", volume.getId(), client.getHostAddress());
        return null;
    }

    private void onConnected(Long id) {
        Volume volume = findById(id);
        if (volume.getStatus().canTransitionTo(VolumeStatus.ATTACHED)) {
            volume.changeStatus(VolumeStatus.ATTACHED, null);
            volumeRepository.save(volume);
        }
    }

    private void onDisconnected(Long id, String reason) {
        Volume volume = findById(id);
        if (volume.getStatus() == VolumeStatus.ATTACHED) {
            volume.changeStatus(VolumeStatus.WAITING, "A conexão com o dispositivo terminou (" + reason + "); ele reconecta sozinho.");
            volumeRepository.save(volume);
        }
    }

    private String attachScript(Volume volume, Device device, String pcAddress, String mountPath) {
        PackageManager manager = device.getPackageManager();
        String nbdPackage = manager == PackageManager.DNF || manager == PackageManager.PACMAN ? "nbd" : "nbd-client";
        return "nbd_host=" + SshService.quote(pcAddress) + "\n"
            + "nbd_port=" + nbdPort + "\n"
            + "nbd_export=" + SshService.quote(volume.getExportName()) + "\n"
            + "volume_id=" + volume.getId() + "\n"
            + "connected=" + (nbdServer != null && nbdServer.isConnected(volume.getId()) ? 1 : 0) + "\n"
            + "mount_path=" + SshService.quote(mountPath) + "\n"
            + "label=" + SshService.quote(volume.getName().length() > 16 ? volume.getName().substring(0, 16) : volume.getName()) + "\n"
            + "allow_format=" + (volume.isFormatted() ? 0 : 1) + "\n"
            + "old_kernel=" + (StorageService.oldKernel(device.getKernelVersion()) ? 1 : 0) + "\n"
            + "install_nbd=" + SshService.quote(manager == null ? "" : manager.installCommand(nbdPackage)) + "\n"
            + "install_e2fs=" + SshService.quote(manager == null ? "" : manager.installCommand("e2fsprogs")) + "\n"
            + deviceScriptService.load("volume-attach");
    }

    private void assign(Long id, Device device, String mountPath, Machine machine, String containerPath) {
        Volume volume = findById(id);
        volume.assign(device, mountPath, machine, containerPath);
        volume.changeStatus(VolumeStatus.ATTACHING, null);
        volumeRepository.save(volume);
    }

    private void finishAttach(Long id, int exitCode, StringBuilder output) {
        Volume volume = findById(id);
        if (exitCode != 0) {
            volume.changeStatus(VolumeStatus.FAILED, lastLine(output.toString()));
        } else if (volume.getStatus() == VolumeStatus.ATTACHING) {
            boolean connected = nbdServer != null && nbdServer.isConnected(id);
            volume.changeStatus(connected ? VolumeStatus.ATTACHED : VolumeStatus.WAITING,
                connected ? null : "Montado, mas a conexão com o PC ainda não foi vista.");
        }
        volumeRepository.save(volume);
    }

    private void finishDetach(Long id, int exitCode, String failure) {
        Volume volume = findById(id);
        if (exitCode != 0) {
            volume.changeStatus(VolumeStatus.FAILED, failure);
            volumeRepository.save(volume);
            return;
        }
        if (nbdServer != null) {
            nbdServer.disconnect(id);
        }
        volume.release();
        volume.changeStatus(VolumeStatus.AVAILABLE, null);
        volumeRepository.save(volume);
    }

    /**
     * For a device that is gone or does not answer: only the panel lets go. The PC stops serving the
     * disk and it becomes free; the device may still show the folder until it reboots.
     */
    public Volume release(Long id) {
        requireHeld(id);
        if (nbdServer != null) {
            nbdServer.disconnect(id);
        }
        Volume volume = findById(id);
        if (volume.getStatus() != VolumeStatus.FAILED) {
            volume.changeStatus(VolumeStatus.FAILED, null);
        }
        volume.release();
        volume.changeStatus(VolumeStatus.AVAILABLE, "Liberado sem falar com o dispositivo; ele pode ainda ter a pasta montada.");
        return volumeRepository.save(volume);
    }

    private Volume requireHeld(Long id) {
        Volume volume = findById(id);
        if (volume.getDevice() == null) {
            throw new IllegalStateException("O disco " + volume.getName() + " não está entregue a nenhum dispositivo.");
        }
        if (volume.getStatus() == VolumeStatus.ATTACHING || volume.getStatus() == VolumeStatus.DETACHING) {
            throw new IllegalStateException("O disco " + volume.getName() + " está " + volume.getStatus().getDescription().toLowerCase() + ".");
        }
        return volume;
    }

    private void markFormatted(Long id) {
        Volume volume = findById(id);
        volume.markFormatted();
        volumeRepository.save(volume);
    }

    private void changeStatus(Long id, VolumeStatus target, String message) {
        Volume volume = findById(id);
        if (volume.getStatus().canTransitionTo(target)) {
            volume.changeStatus(target, message);
            volumeRepository.save(volume);
        }
    }

    private void changeMessage(Long id, String message) {
        Volume volume = findById(id);
        volume.changeStatus(volume.getStatus(), message);
        volumeRepository.save(volume);
    }

    private void requireServer() {
        if (nbdServer == null) {
            throw new IllegalStateException(nbdError != null ? nbdError : "O servidor de discos ainda não subiu.");
        }
    }

    /** Address of this PC on the way to the device: a UDP "connect" only asks the route table. */
    static String localAddressFor(String host) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName(host), 9);
            InetAddress local = socket.getLocalAddress();
            if (local == null || local.isAnyLocalAddress()) {
                throw new IllegalStateException("Este PC não tem rota para " + host + ".");
            }
            return local.getHostAddress();
        } catch (IOException exception) {
            throw new IllegalStateException("Este PC não tem rota para " + host + ": " + exception.getMessage(), exception);
        }
    }

    static void validateMountPath(String path) {
        if (path.contains("..") || path.contains("//") || path.endsWith("/") || MOUNT_ROOTS.stream().noneMatch(path::startsWith)) {
            throw new IllegalArgumentException("Monte o disco dentro de /mnt, /media, /srv, /home, /opt ou /data (por exemplo /mnt/dados).");
        }
        if (SYSTEM_FOLDERS.contains(path)) {
            throw new IllegalArgumentException("Escolha uma pasta dentro de " + path + ", não ela mesma.");
        }
    }

    static void validateContainerPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Informe onde o disco aparece dentro da máquina (por exemplo /dados).");
        }
        if (path.contains("..") || path.contains("//") || path.endsWith("/") || SYSTEM_FOLDERS.contains(path)
            || SYSTEM_PREFIXES.stream().anyMatch(path::startsWith)) {
            throw new IllegalArgumentException("Pasta de sistema da máquina: " + path + ". Use, por exemplo, /dados ou /var/lib/mysql.");
        }
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return bytes;
    }

    private static void deleteQuietly(Path file) {
        try {
            VolumeImage.delete(file);
        } catch (IOException exception) {
            LOG.warn("Could not delete {}: {}", file, exception.getMessage());
        }
    }

    private static String lastLine(String output) {
        String[] lines = output.strip().split("\n");
        for (int index = lines.length - 1; index >= 0; index--) {
            String line = lines[index].trim();
            if (!line.isEmpty() && !line.startsWith("DEVICE=") && !line.startsWith("FORMATTED=")) {
                return line.length() > 500 ? line.substring(0, 500) : line;
            }
        }
        return "Falhou sem mensagem.";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
