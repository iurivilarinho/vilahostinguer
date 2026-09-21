package com.bancada.service;

import com.bancada.enums.AuditAction;
import com.bancada.enums.BackupStatus;
import com.bancada.enums.MachineAction;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.models.Backup;
import com.bancada.models.Machine;
import com.bancada.models.Operation;
import com.bancada.models.PortalSettings;
import com.bancada.models.Subscription;
import com.bancada.records.ServerActionSnapshot;
import com.bancada.repository.SubscriptionRepository;
import com.bancada.request.MachineReinstallRequest;
import com.bancada.response.MachineStatsResponse;
import com.bancada.response.PortalBackupResponse;
import com.bancada.response.ServerResponse;
import com.bancada.specification.SubscriptionSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * What a customer does to their own servers from the panel. Every call starts from the customer
 * id of the session and the subscription id; a server of someone else answers as not found.
 */
@Service
public class PortalServerService {

    private static final String ENTITY = "Subscription";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final CustomerService customerService;
    private final MachineService machineService;
    private final MachineMaintenanceService machineMaintenanceService;
    private final BackupService backupService;
    private final OperationService operationService;
    private final PortalSettingsService portalSettingsService;
    private final PublicIpService publicIpService;
    private final AuditService auditService;

    public PortalServerService(SubscriptionService subscriptionService, SubscriptionRepository subscriptionRepository,
                               CustomerService customerService, MachineService machineService,
                               MachineMaintenanceService machineMaintenanceService, BackupService backupService,
                               OperationService operationService, PortalSettingsService portalSettingsService,
                               PublicIpService publicIpService, AuditService auditService) {
        this.subscriptionService = subscriptionService;
        this.subscriptionRepository = subscriptionRepository;
        this.customerService = customerService;
        this.machineService = machineService;
        this.machineMaintenanceService = machineMaintenanceService;
        this.backupService = backupService;
        this.operationService = operationService;
        this.portalSettingsService = portalSettingsService;
        this.publicIpService = publicIpService;
        this.auditService = auditService;
    }

    /** Servers of the customer that are not canceled, newest first. */
    public List<ServerResponse> list(Long customerId) {
        Specification<Subscription> mine = Specification.where(SubscriptionSpecification.customer(customerId))
            .and(SubscriptionSpecification.statusIn(List.of(SubscriptionStatus.PENDING_PAYMENT, SubscriptionStatus.PROVISIONING,
                SubscriptionStatus.ACTIVE, SubscriptionStatus.SUSPENDED)));
        return subscriptionRepository.findAll(mine, Sort.by("createdAt").descending()).stream()
            .map(this::toResponse)
            .toList();
    }

    public ServerResponse get(Long customerId, Long id) {
        return toResponse(subscriptionService.findForCustomer(id, customerId));
    }

    public Operation action(Long customerId, Long id, MachineAction action) {
        Machine machine = usableMachine(customerId, id);
        Operation operation = machineService.runAction(machine.getId(), action);
        auditService.record(AuditAction.SERVER_ACTION, ENTITY, id, null, new ServerActionSnapshot(action.name(), action.getDescription()), null);
        return operation;
    }

    public MachineStatsResponse stats(Long customerId, Long id) {
        Machine machine = ownedMachine(customerId, id);
        if (machine.getStatus() != MachineStatus.RUNNING) {
            return new MachineStatsResponse(machine.getId(), null, null, null, null);
        }
        return machineService.stats(machine.getDevice().getId()).stream()
            .filter(stats -> stats.machineId().equals(machine.getId()))
            .findFirst()
            .orElse(new MachineStatsResponse(machine.getId(), null, null, null, null));
    }

    /** Reinstall (or change the system); a backup first takes a slot of the plan like any other. */
    public Operation reinstall(Long customerId, Long id, MachineReinstallRequest request) {
        Subscription subscription = subscriptionService.findForCustomer(id, customerId);
        Machine machine = usableMachine(customerId, id);
        if (request.backupFirst()) {
            ensureBackupSlot(subscription, machine);
        }
        Operation operation = machineMaintenanceService.reinstall(machine.getId(), request);
        auditService.record(AuditAction.REINSTALL, ENTITY, id, null,
            new ServerActionSnapshot("REINSTALL", request.distribution().getDisplayName() + " " + request.version()), null);
        return operation;
    }

    public List<PortalBackupResponse> backups(Long customerId, Long id) {
        Machine machine = ownedMachine(customerId, id);
        return backupService.machineBackups(machine.getId()).stream().map(PortalBackupResponse::new).toList();
    }

    public Backup createBackup(Long customerId, Long id, String name) {
        Subscription subscription = subscriptionService.findForCustomer(id, customerId);
        Machine machine = usableMachine(customerId, id);
        ensureBackupSlot(subscription, machine);
        Backup backup = machineMaintenanceService.backup(machine.getId(), name);
        auditService.record(AuditAction.BACKUP, ENTITY, id, null, new ServerActionSnapshot("BACKUP", backup.getName()), null);
        return backup;
    }

    public Operation restore(Long customerId, Long id, Long backupId) {
        Machine machine = usableMachine(customerId, id);
        Backup backup = ownedBackup(machine, backupId);
        Operation operation = machineMaintenanceService.restore(machine.getId(), backup.getId());
        auditService.record(AuditAction.RESTORE, ENTITY, id, null, new ServerActionSnapshot("RESTORE", backup.getName()), null);
        return operation;
    }

    public Backup discardBackup(Long customerId, Long id, Long backupId) {
        Machine machine = ownedMachine(customerId, id);
        Backup backup = ownedBackup(machine, backupId);
        return backupService.changeStatus(backup.getId(), BackupStatus.DISCARDED);
    }

    public Path backupFile(Long customerId, Long id, Long backupId) {
        Machine machine = ownedMachine(customerId, id);
        return backupService.file(ownedBackup(machine, backupId).getId());
    }

    public void changePassword(Long customerId, Long id, String password) {
        Machine machine = usableMachine(customerId, id);
        machineService.changeUserPassword(machine.getId(), password);
        auditService.record(AuditAction.PASSWORD_CHANGE, ENTITY, id, null, null, "Senha do usuário do servidor");
    }

    /** A task is visible when it ran on this server: power, reinstall, restore, or one of its backups. */
    public Operation operation(Long customerId, Long id, Long operationId) {
        Machine machine = ownedMachine(customerId, id);
        Operation operation = operationService.findById(operationId);
        boolean ownTarget = machine.getContainerName().equals(operation.getTarget());
        boolean ownBackup = operation.getTarget() != null && operation.getTarget().startsWith("backup:")
            && backupService.machineBackups(machine.getId()).stream()
                .anyMatch(backup -> operation.getTarget().equals("backup:" + backup.getId()));
        if (!ownTarget && !ownBackup) {
            throw new EntityNotFoundException("Tarefa não encontrada para ID: " + operationId);
        }
        return operation;
    }

    /** Machine for the web terminal: only while the subscription is active. */
    public Machine terminalMachine(Long customerId, Long id) {
        return usableMachine(customerId, id);
    }

    private Machine ownedMachine(Long customerId, Long id) {
        Subscription subscription = subscriptionService.findForCustomer(id, customerId);
        if (subscription.getMachine() == null) {
            throw new IllegalStateException("O servidor ainda não foi criado.");
        }
        return machineService.findById(subscription.getMachine().getId());
    }

    /** Actions need an active subscription, an active account and a machine that is not being prepared. */
    private Machine usableMachine(Long customerId, Long id) {
        customerService.requireActive(customerId);
        Subscription subscription = subscriptionService.findForCustomer(id, customerId);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("O servidor está " + subscription.getStatus().getDescription().toLowerCase() + ".");
        }
        Machine machine = ownedMachine(customerId, id);
        if (machine.getStatus() == MachineStatus.CREATING) {
            throw new IllegalStateException("Espere a tarefa em andamento no servidor terminar.");
        }
        return machine;
    }

    private Backup ownedBackup(Machine machine, Long backupId) {
        Backup backup = backupService.findById(backupId);
        if (backup.getMachine() == null || !backup.getMachine().getId().equals(machine.getId())) {
            throw new EntityNotFoundException("Backup não encontrado para ID: " + backupId);
        }
        return backup;
    }

    private void ensureBackupSlot(Subscription subscription, Machine machine) {
        int slots = subscription.getPlan().getBackupSlots();
        if (backupService.usedMachineSlots(machine.getId()) >= slots) {
            throw new IllegalStateException(slots == 0 ? "O seu plano não inclui backups."
                : "O seu plano guarda até " + slots + " backups. Apague um antes de fazer outro.");
        }
    }

    private ServerResponse toResponse(Subscription subscription) {
        PortalSettings settings = portalSettingsService.get();
        Machine machine = subscription.getMachine();
        String sshHost = settings.getSshHost() != null ? settings.getSshHost()
            : settings.getHostname() != null ? settings.getHostname() : publicIp();
        String sshCommand = subscription.getSshPort() == null || sshHost == null ? null
            : "ssh " + subscription.getUsername() + "@" + sshHost + " -p " + subscription.getSshPort();
        return new ServerResponse(subscription.getId(), subscription.getHostname(), subscription.getPlan().getName(),
            subscription.getPlan().getCpuLimit(), subscription.getPlan().getMemoryMb(), subscription.getPlan().getDiskGb(),
            subscription.getPlan().getBackupSlots(), subscription.getDistribution(), subscription.getDistribution().getDisplayName(),
            subscription.getVersion(), subscription.getUsername(), subscription.getStatus(), subscription.getStatus().getDescription(),
            machine == null ? null : machine.getStatus(), machine == null ? null : machine.getStatus().getDescription(), sshHost,
            subscription.getSshPort(), sshCommand, subscription.getSiteHostname(), subscription.getNextDueDate(),
            subscription.isCancelAtPeriodEnd(), notice(subscription));
    }

    private String publicIp() {
        if (publicIpService.lastKnown() != null) {
            return publicIpService.lastKnown();
        }
        try {
            return publicIpService.current();
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    private static String notice(Subscription subscription) {
        return switch (subscription.getStatus()) {
            case PENDING_PAYMENT -> "Pague a primeira fatura para criarmos o seu servidor.";
            case PROVISIONING -> subscription.getProvisionError() != null ? subscription.getProvisionError()
                : "Estamos criando o seu servidor. Leva alguns minutos.";
            case SUSPENDED -> "Servidor desligado por falta de pagamento. Pague a fatura em aberto para religar.";
            case ACTIVE -> subscription.isCancelAtPeriodEnd()
                ? "Cancelamento agendado: o servidor será apagado em " + DATE.format(subscription.getNextDueDate()) + "."
                : subscription.getProvisionError();
            case CANCELED -> "Assinatura cancelada.";
        };
    }
}
