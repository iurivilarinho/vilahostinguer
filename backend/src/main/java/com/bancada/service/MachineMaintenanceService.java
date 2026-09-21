package com.bancada.service;

import com.bancada.enums.BackupKind;
import com.bancada.enums.BackupStatus;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.OperationType;
import com.bancada.models.Backup;
import com.bancada.models.Device;
import com.bancada.models.Machine;
import com.bancada.models.Operation;
import com.bancada.records.CommandResult;
import com.bancada.request.MachineReinstallRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Whole-machine backup, restore and reinstall. Each runs as one operation with a live log; the
 * machine shows "Preparando" meanwhile and ends running or failed. Shared folders live on the device
 * and are never touched: a reinstall or restore keeps them as they are.
 */
@Service
public class MachineMaintenanceService {

    private final MachineService machineService;
    private final BackupService backupService;
    private final DeviceService deviceService;
    private final SshService sshService;
    private final OperationService operationService;

    public MachineMaintenanceService(MachineService machineService, BackupService backupService, DeviceService deviceService,
                                     SshService sshService, OperationService operationService) {
        this.machineService = machineService;
        this.backupService = backupService;
        this.deviceService = deviceService;
        this.sshService = sshService;
        this.operationService = operationService;
    }

    public Backup backup(Long machineId, String name) {
        Machine machine = machineService.findById(machineId);
        deviceService.requireReady(machine.getDevice().getId());
        return backupService.createForMachine(machine, name);
    }

    /**
     * New system from scratch. With {@code backupFirst}, the whole machine is saved first, and a
     * failed backup stops everything before anything is erased.
     */
    public Operation reinstall(Long machineId, MachineReinstallRequest request) {
        Machine machine = machineService.findById(machineId);
        Device device = deviceService.requireReady(machine.getDevice().getId());
        machineService.validateSystem(device, request.distribution(), request.version());
        Backup safety = request.backupFirst()
            ? backupService.registerMachineBackup(machine, "Antes de reinstalar com " + request.distribution().getDisplayName() + " "
                + request.version())
            : null;
        MachineStatus previous = machineService.startPreparation(machineId);
        String title = "Reinstalar " + machine.getName() + " com " + request.distribution().getDisplayName() + " " + request.version();
        return startOrRevert(machineId, previous, () -> operationService.start(device, OperationType.MACHINE_REINSTALL, title,
            machine.getContainerName(), operationId -> {
                if (safety != null && backupService.produceMachine(safety.getId(), operationId) != 0) {
                    operationService.appendOutput(operationId, "O backup falhou; a máquina não foi apagada.\n");
                    machineService.updateStatus(machineId, previous);
                    return 1;
                }
                int exitCode = 1;
                try {
                    Machine switched = machineService.switchSystem(machineId, request.distribution(), request.version());
                    exitCode = sshService.stream(device, machineService.creationScript(switched, request.password()), true,
                        chunk -> operationService.appendOutput(operationId, chunk),
                        channel -> operationService.registerChannel(operationId, channel));
                    return exitCode;
                } finally {
                    machineService.updateStatus(machineId, exitCode == 0 ? MachineStatus.RUNNING : MachineStatus.FAILED);
                }
            }));
    }

    /**
     * Sends the backup to the device ({@code gunzip | docker import}), then recreates the container
     * from that image. While the upload runs the old machine is untouched: a failed upload keeps it.
     */
    public Operation restore(Long machineId, Long backupId) {
        Machine machine = machineService.findById(machineId);
        Backup backup = backupService.findById(backupId);
        if (backup.getKind() != BackupKind.MACHINE || backup.getMachine() == null || !backup.getMachine().getId().equals(machineId)) {
            throw new IllegalArgumentException("Este backup não é da máquina " + machine.getName() + ".");
        }
        if (backup.getStatus() != BackupStatus.AVAILABLE) {
            throw new IllegalStateException("Só backups disponíveis podem ser restaurados.");
        }
        Path file = Paths.get(backup.getFilePath());
        if (!Files.isReadable(file)) {
            throw new IllegalStateException("O arquivo do backup não está mais em " + file + ".");
        }
        Device device = deviceService.requireReady(machine.getDevice().getId());
        MachineStatus previous = machineService.startPreparation(machineId);
        String image = machineService.restoreImage(machine, backupId);
        return startOrRevert(machineId, previous, () -> operationService.start(device, OperationType.MACHINE_RESTORE,
            "Restaurar " + machine.getName() + ": " + backup.getName(), machine.getContainerName(), operationId -> {
                operationService.appendOutput(operationId, "== Enviando o backup ==\n");
                CommandResult imported;
                try (InputStream input = Files.newInputStream(file)) {
                    imported = sshService.upload(device, "gunzip -c | docker import - " + SshService.quote(image), true, input,
                        channel -> operationService.registerChannel(operationId, channel));
                } catch (IOException exception) {
                    machineService.updateStatus(machineId, previous);
                    throw new IllegalStateException("Não foi possível ler o arquivo do backup: " + exception.getMessage(), exception);
                }
                appendIfPresent(operationId, imported.errorOutput());
                if (!imported.succeeded()) {
                    operationService.appendOutput(operationId, "O envio falhou; a máquina continua como estava.\n");
                    machineService.updateStatus(machineId, previous);
                    return imported.exitCode();
                }
                int exitCode = 1;
                try {
                    Machine restored = machineService.useImage(machineId, image);
                    exitCode = sshService.stream(device, machineService.restoreScript(restored, image), true,
                        chunk -> operationService.appendOutput(operationId, chunk),
                        channel -> operationService.registerChannel(operationId, channel));
                    return exitCode;
                } finally {
                    machineService.updateStatus(machineId, exitCode == 0 ? MachineStatus.RUNNING : MachineStatus.FAILED);
                }
            }));
    }

    /** An operation that never started must not leave the machine stuck as "Preparando". */
    private Operation startOrRevert(Long machineId, MachineStatus previous, Supplier<Operation> start) {
        try {
            return start.get();
        } catch (RuntimeException exception) {
            machineService.updateStatus(machineId, previous);
            throw exception;
        }
    }

    private void appendIfPresent(Long operationId, String text) {
        if (text != null && !text.isBlank()) {
            operationService.appendOutput(operationId, text.endsWith("\n") ? text : text + "\n");
        }
    }
}
