package com.bancada.service;

import com.bancada.enums.MachineAction;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineNetworkMode;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.OperationType;
import com.bancada.filter.MachineFilter;
import com.bancada.models.Device;
import com.bancada.models.Machine;
import com.bancada.models.MachinePort;
import com.bancada.models.MachineVolume;
import com.bancada.models.Operation;
import com.bancada.records.CommandResult;
import com.bancada.records.KeyValueOutput;
import com.bancada.records.MachineStatusChangedEvent;
import com.bancada.repository.MachineRepository;
import com.bancada.request.MachineRequest;
import com.bancada.response.DistributionResponse;
import com.bancada.response.DockerStatusResponse;
import com.bancada.response.MachineCreationResponse;
import com.bancada.response.MachineLogsResponse;
import com.bancada.response.MachineResponse;
import com.bancada.response.MachineStatsResponse;
import com.bancada.specification.MachineSpecification;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Linux "machines": long-running Docker system containers. PID 1 is a tiny shell loop that starts
 * the SSH server (when installed) and then waits forever, so the machine survives restarts of the
 * device with its SSH working, without depending on systemd inside the container.
 */
@Service
public class MachineService {

    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(45);
    private static final String LABEL = "bancada.machine";
    private static final String START_SSHD = "mkdir -p /run/sshd; chown root:root /run/sshd; chmod 755 /run/sshd; "
        + "pkill -x sshd 2>/dev/null; /usr/sbin/sshd";
    private static final String ENTRYPOINT = "umask 022; if [ -x /usr/sbin/sshd ]; then " + START_SSHD + "; fi; exec tail -f /dev/null";
    private static final int LOG_LINES = 300;
    private static final String RESTORE_REPOSITORY = "bancada-restore/";

    private final MachineRepository machineRepository;
    private final DeviceService deviceService;
    private final SshService sshService;
    private final OperationService operationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public MachineService(MachineRepository machineRepository, DeviceService deviceService, SshService sshService,
                          OperationService operationService, ApplicationEventPublisher applicationEventPublisher) {
        this.machineRepository = machineRepository;
        this.deviceService = deviceService;
        this.sshService = sshService;
        this.operationService = operationService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /** A creation interrupted by closing the app can never finish. */
    @PostConstruct
    public void failInterruptedCreations() {
        for (Machine machine : machineRepository.findByStatus(MachineStatus.CREATING)) {
            machine.changeStatus(MachineStatus.FAILED);
            machineRepository.save(machine);
        }
    }

    @Transactional(readOnly = true)
    public Page<Machine> search(MachineFilter filter, Pageable pageable) {
        Specification<Machine> specification = Specification.where(MachineSpecification.device(filter.getDeviceId()))
            .and(MachineSpecification.search(filter.getSearch()))
            .and(MachineSpecification.statusIn(filter.getStatus()));
        return machineRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Machine findById(Long id) {
        return machineRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Máquina não encontrada para ID: " + id));
    }

    public List<DistributionResponse> distributions(Long deviceId) {
        Device device = deviceService.findById(deviceId);
        return Arrays.stream(MachineDistribution.values())
            .map(distribution -> new DistributionResponse(distribution, distribution.getDisplayName(), distribution.getVersions(),
                distribution.supports(device.getArchitecture())))
            .toList();
    }

    /** Checks the Docker client, the daemon and the kernel features Docker cannot live without. */
    public DockerStatusResponse dockerStatus(Long deviceId) {
        Device device = deviceService.requireReady(deviceId);
        String script = String.join("\n",
            "if command -v docker >/dev/null 2>&1; then echo installed=1; fi",
            "if version=$(docker version --format '{{.Server.Version}}' 2>/tmp/bancada-docker.err); then",
            "  echo running=1; echo \"version=$version\"",
            "else",
            "  echo \"message=$(head -c 400 /tmp/bancada-docker.err 2>/dev/null | tr '\\n' ' ')\"",
            "fi",
            "missing=''",
            "for controller in memory devices cpuset; do",
            "  awk -v c=$controller '$1==c && $4==1 {found=1} END {exit !found}' /proc/cgroups || missing=\"$missing,cgroup $controller\"",
            "done",
            "grep -qw overlay /proc/filesystems || modprobe overlay 2>/dev/null || missing=\"$missing,overlayfs\"",
            "echo \"missing=${missing#,}\"");
        CommandResult result = sshService.run(device, script, List.of(), true, QUERY_TIMEOUT);
        KeyValueOutput output = KeyValueOutput.parse(result.output());
        String missing = output.text("missing");
        List<String> missingFeatures = missing == null ? List.of() : Arrays.asList(missing.split(","));
        return new DockerStatusResponse(output.flag("installed"), output.flag("running"), output.text("version"), missingFeatures,
            output.text("message"));
    }

    public MachineCreationResponse create(MachineRequest request) {
        Device device = deviceService.requireReady(request.deviceId());
        validateSystem(device, request.distribution(), request.version());
        if (request.networkMode() == MachineNetworkMode.HOST && request.installSsh()
            && (request.sshPort() == null || request.sshPort() == 22)) {
            throw new IllegalArgumentException("Na rede do dispositivo a porta 22 já é dele. Escolha outra porta para o SSH da máquina.");
        }
        String containerName = Machine.CONTAINER_PREFIX + request.name();
        if (machineRepository.existsByDeviceIdAndContainerNameAndStatusNot(device.getId(), containerName, MachineStatus.REMOVED)) {
            throw new DataIntegrityViolationException("Já existe uma máquina chamada \"" + request.name() + "\" neste dispositivo.");
        }
        Machine machine = machineRepository.save(new Machine(request, device));
        Long machineId = machine.getId();
        String script = creationScript(machine, request.password());
        Operation operation;
        try {
            operation = startCreation(device, machine, machineId, script);
        } catch (RuntimeException e) {
            // nothing reached the device: the record must not keep the name taken
            forceStatus(machineId, MachineStatus.REMOVED);
            throw e;
        }
        return new MachineCreationResponse(new MachineResponse(machine), operation.getId());
    }

    private Operation startCreation(Device device, Machine machine, Long machineId, String script) {
        return operationService.start(device, OperationType.MACHINE_CREATE, "Criar máquina " + machine.getName(),
            machine.getContainerName(),
            operationId -> {
                int exitCode = 1;
                try {
                    exitCode = sshService.stream(device, script, true,
                        chunk -> operationService.appendOutput(operationId, chunk),
                        channel -> operationService.registerChannel(operationId, channel));
                    return exitCode;
                } finally {
                    updateStatus(machineId, exitCode == 0 ? MachineStatus.RUNNING : MachineStatus.FAILED);
                }
            });
    }

    public Operation runAction(Long id, MachineAction action) {
        Machine machine = findById(id);
        if (machine.getStatus() == MachineStatus.REMOVED || machine.getStatus() == MachineStatus.CREATING) {
            throw new IllegalStateException("A máquina " + machine.getName() + " está " + machine.getStatus().getDescription().toLowerCase() + ".");
        }
        Device device = deviceService.requireReady(machine.getDevice().getId());
        String script = "docker " + action.getDockerCommand() + " " + SshService.quote(machine.getContainerName()) + " || exit $?\n"
            + "echo '== " + action.getDescription() + ": concluído =='\n";
        return operationService.start(device, OperationType.MACHINE_ACTION, action.getDescription() + " " + machine.getName(),
            machine.getContainerName(), operationId -> {
                int exitCode = sshService.stream(device, script, true,
                    chunk -> operationService.appendOutput(operationId, chunk),
                    channel -> operationService.registerChannel(operationId, channel));
                if (exitCode == 0) {
                    updateStatus(id, action.getResultingStatus());
                }
                return exitCode;
            });
    }

    public Operation remove(Long id) {
        Machine machine = findById(id);
        if (machine.getStatus() == MachineStatus.REMOVED) {
            throw new IllegalStateException("A máquina já foi removida.");
        }
        Device device = deviceService.requireReady(machine.getDevice().getId());
        String script = "docker rm -f " + SshService.quote(machine.getContainerName()) + " >/dev/null 2>&1 || true\n"
            + "echo '== Máquina " + machine.getName() + " removida. Pastas compartilhadas não foram apagadas. =='\n";
        return operationService.start(device, OperationType.MACHINE_REMOVE, "Remover máquina " + machine.getName(),
            machine.getContainerName(), operationId -> {
                int exitCode = sshService.stream(device, script, true,
                    chunk -> operationService.appendOutput(operationId, chunk),
                    channel -> operationService.registerChannel(operationId, channel));
                forceStatus(id, MachineStatus.REMOVED);
                return exitCode;
            });
    }

    /** Reads the real container states and fixes the stored status of the device machines. */
    public void syncStatuses(Long deviceId) {
        Device device = deviceService.requireReady(deviceId);
        CommandResult result = sshService.run(device,
            "docker ps -a --filter label=" + LABEL + " --format '{{.Names}}|{{.State}}' 2>/dev/null", List.of(), true, QUERY_TIMEOUT);
        if (!result.succeeded()) {
            return;
        }
        Map<String, String> states = new HashMap<>();
        for (String line : result.output().split("\n")) {
            String[] fields = line.trim().split("\\|");
            if (fields.length == 2) {
                states.put(fields[0], fields[1]);
            }
        }
        for (Machine machine : machineRepository.findByDeviceIdAndStatusNot(deviceId, MachineStatus.REMOVED)) {
            if (machine.getStatus() == MachineStatus.CREATING) {
                continue;
            }
            String state = states.get(machine.getContainerName());
            MachineStatus actual = state == null ? MachineStatus.FAILED : "running".equals(state) ? MachineStatus.RUNNING : MachineStatus.STOPPED;
            if (actual != machine.getStatus()) {
                machine.changeStatus(actual);
                machineRepository.save(machine);
                applicationEventPublisher.publishEvent(new MachineStatusChangedEvent(machine.getId(), actual));
            }
        }
    }

    public List<MachineStatsResponse> stats(Long deviceId) {
        Device device = deviceService.requireReady(deviceId);
        List<Machine> machines = machineRepository.findByDeviceIdAndStatusNot(deviceId, MachineStatus.REMOVED);
        if (machines.isEmpty()) {
            return List.of();
        }
        CommandResult result = sshService.run(device,
            "docker stats --no-stream --format '{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}|{{.PIDs}}' 2>/dev/null",
            List.of(), true, QUERY_TIMEOUT);
        Map<String, String[]> byName = new HashMap<>();
        for (String line : result.output().split("\n")) {
            String[] fields = line.trim().split("\\|");
            if (fields.length == 5) {
                byName.put(fields[0], fields);
            }
        }
        List<MachineStatsResponse> stats = new ArrayList<>();
        for (Machine machine : machines) {
            String[] fields = byName.get(machine.getContainerName());
            if (fields != null) {
                stats.add(new MachineStatsResponse(machine.getId(), parsePercent(fields[1]), fields[2], parsePercent(fields[3]),
                    parseInteger(fields[4])));
            }
        }
        return stats;
    }

    public MachineLogsResponse logs(Long id) {
        Machine machine = findById(id);
        Device device = deviceService.requireReady(machine.getDevice().getId());
        CommandResult result = sshService.run(device,
            "docker logs --tail " + LOG_LINES + " " + SshService.quote(machine.getContainerName()) + " 2>&1", List.of(), true, QUERY_TIMEOUT);
        return new MachineLogsResponse(machine.getId(), result.output());
    }

    /** New password for the machine user (SSH and sudo). */
    public void changeUserPassword(Long id, String password) {
        Machine machine = findById(id);
        if (machine.getStatus() != MachineStatus.RUNNING) {
            throw new IllegalStateException("Ligue o servidor antes de trocar a senha.");
        }
        Device device = deviceService.requireReady(machine.getDevice().getId());
        String script = "printf '%s:%s\\n' " + SshService.quote(machine.getUsername()) + " " + SshService.quote(password)
            + " | docker exec -i " + SshService.quote(machine.getContainerName()) + " chpasswd\n";
        CommandResult result = sshService.run(device, script, List.of(), true, QUERY_TIMEOUT);
        if (!result.succeeded()) {
            throw new IllegalStateException("A senha não foi trocada: " + result.errorOutput().trim());
        }
    }

    /** Command that opens an interactive shell inside the machine, for the web terminal. */
    public String shellCommand(Machine machine) {
        return "docker exec -it " + SshService.quote(machine.getContainerName())
            + " sh -c 'umask 022; if command -v bash >/dev/null 2>&1; then exec bash -l; else exec sh -l; fi'";
    }

    /** Fresh install: pull the image, start the container and prepare user, sudo and SSH. */
    public String creationScript(Machine machine, String password) {
        String name = SshService.quote(machine.getContainerName());
        String image = SshService.quote(machine.getImage());
        return preamble(machine)
            + "echo '== Baixando a imagem " + machine.getImage() + " =='\n"
            + "docker pull " + image + " || exit $?\n"
            + "docker rm -f " + name + " >/dev/null 2>&1\n"
            + "echo '== Criando a máquina =='\n"
            + runCommand(machine, image) + " || exit $?\n"
            + provisionScript(machine, password)
            + "echo '== Máquina pronta =='\n";
    }

    /**
     * Restore: the image imported from a backup already carries the system, the user and the SSH
     * configuration; only the container is recreated from it, with the same resources and ports.
     */
    public String restoreScript(Machine machine, String importedImage) {
        String name = SshService.quote(machine.getContainerName());
        String image = SshService.quote(importedImage);
        return preamble(machine)
            + "docker rm -f " + name + " >/dev/null 2>&1\n"
            + "echo '== Recriando a máquina a partir do backup =='\n"
            + runCommand(machine, image) + " || exit $?\n"
            + "for old in $(docker images " + SshService.quote(RESTORE_REPOSITORY + machine.getContainerName())
            + " --format '{{.Repository}}:{{.Tag}}'); do\n"
            + "  [ \"$old\" = " + image + " ] || docker rmi \"$old\" >/dev/null 2>&1\n"
            + "done\n"
            + "echo '== Máquina restaurada =='\n";
    }

    /** Local image name for a machine backup imported on the device. */
    public String restoreImage(Machine machine, Long backupId) {
        return RESTORE_REPOSITORY + machine.getContainerName() + ":b" + backupId;
    }

    /** Docker checks, CPU limit flag, proxy for the machine and shared folders. */
    private String preamble(Machine machine) {
        StringBuilder script = new StringBuilder()
            .append("command -v docker >/dev/null 2>&1 || { echo 'O Docker não está instalado neste dispositivo.'; exit 127; }\n")
            .append("docker info >/dev/null 2>&1 || { echo 'O Docker não está respondendo. Veja a aba Aplicativos.'; exit 1; }\n");
        if (machine.getCpuLimit() != null) {
            // kernels without CFS quota (Android 3.x) reject --cpus; a proportional weight is the closest
            int shares = (int) Math.max(2, Math.round(machine.getCpuLimit() * 1024));
            script.append("cpu_limit=")
                .append(SshService.quote("--cpus " + String.format(Locale.ROOT, "%.2f", machine.getCpuLimit()))).append('\n')
                .append("if docker info 2>&1 | grep -q 'No cpu cfs quota'; then\n")
                .append("  echo 'Aviso: o kernel não limita CPU por cota; a máquina recebe peso proporcional (--cpu-shares).'\n")
                .append("  cpu_limit='--cpu-shares ").append(shares).append("'\n")
                .append("fi\n");
        }
        // an isolated machine cannot reach the device loopback: its proxy is the bridge gateway (see docker-setup.sh)
        script.append("machine_proxy=\"${http_proxy:-}\"\n");
        if (machine.getNetworkMode() == MachineNetworkMode.BRIDGE) {
            script.append("case \"$machine_proxy\" in *127.0.0.1*|*localhost*)\n")
                .append("  gateway=$(docker network inspect bridge -f '{{range .IPAM.Config}}{{.Gateway}}{{end}}' 2>/dev/null)\n")
                .append("  machine_proxy=$(printf '%s' \"$machine_proxy\" | sed \"s#127\\.0\\.0\\.1#${gateway:-172.17.0.1}#; s#localhost#${gateway:-172.17.0.1}#\")\n")
                .append("  [ -x /usr/local/sbin/bancada-bridge-proxy ] && /usr/local/sbin/bancada-bridge-proxy >/dev/null 2>&1\n")
                .append(";;\nesac\n");
        }
        for (MachineVolume volume : machine.getVolumes()) {
            script.append("mkdir -p ").append(SshService.quote(volume.getHostPath())).append('\n');
        }
        return script.toString();
    }

    private String runCommand(Machine machine, String quotedImage) {
        StringBuilder run = new StringBuilder("docker run -d --name ").append(SshService.quote(machine.getContainerName()))
            .append(" --hostname ").append(SshService.quote(machine.getName()))
            .append(" --label ").append(LABEL).append('=').append(machine.getId())
            .append(" --restart ").append(machine.isAutoStart() ? "unless-stopped" : "no");
        if (machine.getCpuLimit() != null) {
            run.append(" $cpu_limit");
        }
        if (machine.getMemoryLimitMb() != null) {
            run.append(" --memory ").append(machine.getMemoryLimitMb()).append('m');
        }
        if (machine.getNetworkMode() == MachineNetworkMode.HOST) {
            // on the host network Docker does not put the machine name in /etc/hosts (sudo complains)
            run.append(" --network host --add-host ").append(SshService.quote(machine.getName() + ":127.0.1.1"));
        }
        for (MachinePort port : machine.getPorts()) {
            run.append(" -p ").append(port.getHostPort()).append(':').append(port.getContainerPort()).append('/').append(port.getProtocol());
        }
        for (MachineVolume volume : machine.getVolumes()) {
            run.append(" -v ").append(SshService.quote(volume.getHostPath() + ":" + volume.getContainerPath()));
        }
        run.append(" ${machine_proxy:+-e http_proxy=$machine_proxy -e https_proxy=$machine_proxy -e HTTP_PROXY=$machine_proxy"
            + " -e HTTPS_PROXY=$machine_proxy -e no_proxy=localhost,127.0.0.1}");
        run.append(" --entrypoint sh ").append(quotedImage).append(" -c ").append(SshService.quote(ENTRYPOINT));
        return run.toString();
    }

    /** Inside a fresh container: basic packages, proxy profile, user with sudo, shared folders and SSH. */
    private String provisionScript(Machine machine, String password) {
        String name = SshService.quote(machine.getContainerName());
        String user = machine.getUsername();
        StringBuilder script = new StringBuilder()
            .append("echo '== Instalando o básico do sistema (pode levar alguns minutos) =='\n")
            .append("docker exec ${machine_proxy:+-e http_proxy=$machine_proxy -e https_proxy=$machine_proxy} ").append(name)
            .append(inside(machine.getDistribution().packageInstallCommand(machine.isSshEnabled()))).append(" || exit $?\n")
            .append("if [ -n \"$machine_proxy\" ]; then\n")
            .append("  docker exec -e machine_proxy=\"$machine_proxy\" ").append(name)
            .append(inside("mkdir -p /etc/profile.d && echo \"export http_proxy=$machine_proxy https_proxy=$machine_proxy "
                + "no_proxy=localhost,127.0.0.1\" > /etc/profile.d/proxy.sh")).append('\n')
            .append("fi\n")
            .append("echo '== Criando o usuário ").append(user).append(" =='\n")
            .append("docker exec ").append(name)
            .append(inside("id -u " + user + " >/dev/null 2>&1 || useradd -m -s /bin/bash " + user
                + " 2>/dev/null || adduser -D -s /bin/bash " + user)).append(" || exit $?\n")
            .append("printf '%s:%s\\n' ").append(SshService.quote(user)).append(' ').append(SshService.quote(password))
            .append(" | docker exec -i ").append(name).append(" chpasswd || exit $?\n")
            .append("docker exec ").append(name)
            .append(inside("mkdir -p /etc/sudoers.d && echo '" + user + " ALL=(ALL) ALL' > /etc/sudoers.d/" + user
                + " && chmod 440 /etc/sudoers.d/" + user)).append(" || exit $?\n");
        // a shared folder that is still empty was just created by this script: hand it to the user
        for (MachineVolume volume : machine.getVolumes()) {
            String path = SshService.quote(volume.getContainerPath());
            script.append("docker exec ").append(name)
                .append(inside("[ -z \"$(ls -A " + path + ")\" ] && chown " + user + ": " + path + "; true")).append('\n');
        }
        if (machine.isSshEnabled()) {
            String port = String.valueOf(machine.getSshPort());
            script.append("echo '== Ligando o SSH na porta ").append(port).append(" =='\n")
                .append("docker exec ").append(name)
                .append(inside("ssh-keygen -A >/dev/null 2>&1; sed -i -e '/^#\\?Port /d' /etc/ssh/sshd_config; "
                    + "echo 'Port " + port + "' >> /etc/ssh/sshd_config; " + START_SSHD))
                .append(" || exit $?\n");
        }
        return script.toString();
    }

    /**
     * Command run by {@code sh} inside the machine with umask 022: this Docker hands docker exec an
     * umask of 000, which would leave every file created world-writable (and sshd refuses to start).
     */
    private static String inside(String command) {
        return " sh -c " + SshService.quote("umask 022; " + command);
    }

    /** Applies the status when the lifecycle allows it (a removed machine stays removed). */
    public void updateStatus(Long id, MachineStatus target) {
        Machine machine = findById(id);
        if (machine.getStatus().canTransitionTo(target)) {
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

    @Transactional
    public Machine useImage(Long id, String image) {
        Machine machine = findById(id);
        machine.useImage(image);
        return machineRepository.save(machine);
    }

    /** Distribution and version must exist and have an image for the processor of the device. */
    public void validateSystem(Device device, MachineDistribution distribution, String version) {
        if (!distribution.supports(device.getArchitecture())) {
            throw new IllegalArgumentException(distribution.getDisplayName() + " não tem imagem para " + device.getArchitecture() + ".");
        }
        if (!distribution.getVersions().contains(version)) {
            throw new IllegalArgumentException("Versão não oferecida: " + version);
        }
    }

    /** Removal always ends as REMOVED, whatever state the container was in. */
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

    private static Double parsePercent(String value) {
        try {
            return Double.parseDouble(value.replace("%", "").trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
