package com.bancada.service;

import com.bancada.enums.FileSystemType;
import com.bancada.enums.OperationType;
import com.bancada.models.Device;
import com.bancada.models.Operation;
import com.bancada.records.CommandResult;
import com.bancada.request.FormatPartitionRequest;
import com.bancada.response.PartitionResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Lists partitions and formats the ones that are safe to format.
 *
 * <p>On a phone most partitions are unmounted and look free: modem, efs, persist, boot. Formatting
 * one of those bricks the device. So a named (GPT) partition is only offered when its name is a
 * known data partition; unnamed partitions (SD cards, USB drives) are offered when unmounted.
 */
@Service
public class StorageService {

    private static final Duration LIST_TIMEOUT = Duration.ofSeconds(30);
    private static final Set<String> DATA_PARTITION_NAMES = Set.of("userdata", "data", "storage", "sdcard", "media");
    private static final int FIELD_COUNT = 8;

    private final DeviceService deviceService;
    private final SshService sshService;
    private final DeviceScriptService deviceScriptService;
    private final OperationService operationService;

    public StorageService(DeviceService deviceService, SshService sshService, DeviceScriptService deviceScriptService,
                          OperationService operationService) {
        this.deviceService = deviceService;
        this.sshService = sshService;
        this.deviceScriptService = deviceScriptService;
        this.operationService = operationService;
    }

    public List<PartitionResponse> partitions(Long deviceId) {
        Device device = deviceService.requireReady(deviceId);
        CommandResult result = sshService.run(device, deviceScriptService.load("partitions"), List.of(), true, LIST_TIMEOUT);
        List<PartitionResponse> partitions = new ArrayList<>();
        for (String line : result.output().split("\n")) {
            String[] fields = line.split("\\|", -1);
            if (fields.length < FIELD_COUNT || fields[0].isBlank()) {
                continue;
            }
            String name = fields[0];
            long size = parseLong(fields[1]);
            boolean disk = "1".equals(fields[2]);
            String fileSystem = blankToNull(fields[3]);
            String label = blankToNull(fields[4]);
            String partitionName = blankToNull(fields[5]);
            String mountPoint = blankToNull(fields[6]);
            Long used = fields[7].isBlank() ? null : parseLong(fields[7]);
            String protection = protectionReason(disk, mountPoint, partitionName, fileSystem);
            partitions.add(new PartitionResponse(name, size, disk, fileSystem, label, partitionName, mountPoint, used,
                protection == null, protection));
        }
        // The root filesystem is always mounted somewhere. If no partition shows a mount point, the mount
        // table could not be read, and "unmounted" means nothing: refuse everything instead of guessing.
        if (partitions.stream().noneMatch(partition -> partition.mountPoint() != null)) {
            return partitions.stream()
                .map(partition -> new PartitionResponse(partition.name(), partition.sizeBytes(), partition.disk(),
                    partition.fileSystem(), partition.label(), partition.partitionName(), partition.mountPoint(),
                    partition.usedBytes(), false, "Não foi possível ler os pontos de montagem deste sistema."))
                .toList();
        }
        return partitions;
    }

    public Operation format(Long deviceId, FormatPartitionRequest request) {
        if (!request.partition().equals(request.confirmation().trim())) {
            throw new IllegalArgumentException("A confirmação não confere com o nome da partição.");
        }
        Device device = deviceService.requireReady(deviceId);
        PartitionResponse partition = partitions(deviceId).stream()
            .filter(candidate -> candidate.name().equals(request.partition()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Partição não encontrada: " + request.partition()));
        if (!partition.formattable()) {
            throw new IllegalStateException("A partição " + partition.name() + " não pode ser formatada: " + partition.protectionReason());
        }
        String script = formatScript(device, request);
        return operationService.start(device, OperationType.FORMAT_PARTITION,
            "Formatar " + partition.name() + " como " + request.fileSystem().getLabel(), partition.name(),
            operationId -> sshService.stream(device, script, true,
                chunk -> operationService.appendOutput(operationId, chunk),
                channel -> operationService.registerChannel(operationId, channel)));
    }

    private static String formatScript(Device device, FormatPartitionRequest request) {
        String node = "/dev/" + request.partition();
        String label = request.label() == null ? "" : request.label();
        FileSystemType fileSystem = request.fileSystem();
        StringBuilder script = new StringBuilder()
            .append("node=").append(SshService.quote(node)).append('\n')
            .append("[ -b \"$node\" ] || { echo \"Dispositivo de bloco não encontrado: $node\"; exit 2; }\n")
            .append("if grep -q \"^$node \" /proc/mounts; then echo \"A partição está montada; desmonte antes.\"; exit 3; fi\n")
            .append("command -v ").append(fileSystem.getCommand())
            .append(" >/dev/null 2>&1 || { echo \"").append(fileSystem.getCommand())
            .append(" não está instalado no dispositivo.\"; exit 127; }\n")
            .append("echo \"== Formatando $node como ").append(fileSystem.getLabel()).append(" ==\"\n");
        if (fileSystem == FileSystemType.EXT4) {
            String labelOption = label.isEmpty() ? "" : " -L " + SshService.quote(label);
            if (oldKernel(device.getKernelVersion())) {
                // A 3.x kernel cannot mount ext4 created with the features current e2fsprogs enables by default.
                script.append("echo 'Kernel antigo: desligando metadata_csum_seed e orphan_file para ele conseguir montar.'\n")
                    .append("mkfs.ext4 -F").append(labelOption).append(" -O ^metadata_csum_seed,^orphan_file \"$node\" ")
                    .append("|| mkfs.ext4 -F").append(labelOption).append(" -O ^metadata_csum_seed \"$node\" || exit $?\n");
            } else {
                script.append("mkfs.ext4 -F").append(labelOption).append(" \"$node\" || exit $?\n");
            }
        } else {
            String labelOption = label.isEmpty() ? "" : " -n " + SshService.quote(label.toUpperCase());
            script.append("mkfs.vfat -F 32").append(labelOption).append(" \"$node\" || exit $?\n");
        }
        script.append("sync\necho '== Concluído =='\n");
        return script.toString();
    }

    private static String protectionReason(boolean disk, String mountPoint, String partitionName, String fileSystem) {
        if (disk) {
            return "É o disco inteiro, não uma partição.";
        }
        if (mountPoint != null) {
            return "[swap]".equals(mountPoint) ? "Está em uso como swap." : "Está montada em " + mountPoint + ".";
        }
        if ("swap".equals(fileSystem)) {
            return "É uma partição de swap.";
        }
        if (partitionName != null && !DATA_PARTITION_NAMES.contains(partitionName.toLowerCase())) {
            return "Partição de sistema do aparelho (" + partitionName + "). Formatar pode inutilizá-lo.";
        }
        return null;
    }

    private static boolean oldKernel(String kernelVersion) {
        if (kernelVersion == null) {
            return false;
        }
        try {
            return Integer.parseInt(kernelVersion.split("\\.")[0]) < 4;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
