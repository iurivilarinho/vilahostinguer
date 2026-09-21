package com.bancada.service;

import com.bancada.enums.BackupKind;
import com.bancada.enums.BackupStatus;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.OperationType;
import com.bancada.hyperv.CloudInit;
import com.bancada.models.Backup;
import com.bancada.models.Machine;
import com.bancada.models.Operation;
import com.bancada.request.MachineReinstallRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Whole-machine backup, restore and reinstall. Each runs as one operation with a live log; the
 * machine shows "Preparando" meanwhile and ends running or failed. A reinstall keeps the address,
 * the host key and the panel access, so the device of the machine stays the same.
 */
@Service
public class MachineMaintenanceService {

    private final MachineService machineService;
    private final BackupService backupService;
    private final HyperVService hyperVService;
    private final SshService sshService;
    private final OperationService operationService;

    public MachineMaintenanceService(MachineService machineService, BackupService backupService, HyperVService hyperVService,
                                     SshService sshService, OperationService operationService) {
        this.machineService = machineService;
        this.backupService = backupService;
        this.hyperVService = hyperVService;
        this.sshService = sshService;
        this.operationService = operationService;
    }

    public Backup backup(Long machineId, String name) {
        Machine machine = machineService.findById(machineId);
        hyperVService.requireReady();
        return backupService.createForMachine(machine, name);
    }

    /**
     * New system from scratch, same or another distribution. With {@code backupFirst}, the disk is
     * copied first, and a failed copy stops everything before anything is erased.
     */
    public Operation reinstall(Long machineId, MachineReinstallRequest request) {
        Machine machine = machineService.findById(machineId);
        hyperVService.requireReady();
        machineService.validateSystem(request.distribution(), request.version());
        Backup safety = request.backupFirst()
            ? backupService.registerMachineBackup(machine, "Antes de reinstalar com " + request.distribution().getDisplayName() + " "
                + request.version())
            : null;
        MachineStatus previous = machineService.startPreparation(machineId);
        String title = "Reinstalar " + machine.getName() + " com " + request.distribution().getDisplayName() + " " + request.version();
        return startOrRevert(machineId, previous, () -> operationService.start(machine.getDevice(), OperationType.MACHINE_REINSTALL, title,
            machine.getVmName(), operationId -> {
                if (safety != null && backupService.produceMachine(safety.getId(), operationId) != 0) {
                    operationService.appendOutput(operationId, "O backup falhou; a máquina não foi apagada.\n");
                    machineService.updateStatus(machineId, previous);
                    return 1;
                }
                int exitCode = 1;
                try {
                    Machine switched = machineService.switchSystem(machineId, request.distribution(), request.version());
                    CloudInit seed = machineService.cloudInit(switched, request.password(), machineService.keysOf(switched));
                    exitCode = machineService.install(operationId, machineId, seed, false);
                    return exitCode;
                } finally {
                    machineService.updateStatus(machineId, exitCode == 0 ? MachineStatus.RUNNING : MachineStatus.FAILED);
                }
            }));
    }

    /** The disk goes back to the copy in the backup; everything written after it is lost. */
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
        hyperVService.requireReady();
        MachineStatus previous = machineService.startPreparation(machineId);
        return startOrRevert(machineId, previous, () -> operationService.start(machine.getDevice(), OperationType.MACHINE_RESTORE,
            "Restaurar " + machine.getName() + ": " + backup.getName(), machine.getVmName(), operationId -> {
                int exitCode = 1;
                try {
                    sshService.invalidate(machine.getDevice().getId());
                    exitCode = hyperVService.restoreDisk(machine.getVmName(), file, machine.diskPath(),
                        chunk -> operationService.appendOutput(operationId, chunk));
                    if (exitCode == 0) {
                        operationService.appendOutput(operationId, "== Máquina restaurada ==\n");
                    }
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
}
