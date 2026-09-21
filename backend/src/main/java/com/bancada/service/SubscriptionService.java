package com.bancada.service;

import com.bancada.enums.AuditAction;
import com.bancada.enums.MachineAction;
import com.bancada.enums.MachineNetworkMode;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.RouteStatus;
import com.bancada.enums.RouteType;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.filter.SubscriptionFilter;
import com.bancada.models.Customer;
import com.bancada.models.Invoice;
import com.bancada.models.Machine;
import com.bancada.models.MachinePort;
import com.bancada.models.Plan;
import com.bancada.models.PortalSettings;
import com.bancada.models.Route;
import com.bancada.models.Subscription;
import com.bancada.records.InvoicePaidEvent;
import com.bancada.records.MachineStatusChangedEvent;
import com.bancada.records.ServerPorts;
import com.bancada.records.SubscriptionSnapshot;
import com.bancada.repository.MachineRepository;
import com.bancada.repository.SubscriptionRepository;
import com.bancada.request.CancelSubscriptionRequest;
import com.bancada.request.CheckoutRequest;
import com.bancada.request.MachinePortRequest;
import com.bancada.request.MachineReinstallRequest;
import com.bancada.request.MachineRequest;
import com.bancada.request.RouteRequest;
import com.bancada.response.CheckoutResponse;
import com.bancada.response.InvoiceResponse;
import com.bancada.response.MachineCreationResponse;
import com.bancada.response.SubscriptionResponse;
import com.bancada.specification.SubscriptionSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Life of a contracted server: checkout, creation once paid, renewals, suspension for late payment,
 * cancellation. The server is a machine on an isolated network with three ports of its own on the
 * device (SSH, 80 and 443), published through the gateway.
 */
@Service
public class SubscriptionService {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionService.class);
    private static final String ENTITY = "Subscription";
    private static final int SSH_PORT = 22;
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;
    private static final int UNPAID_ORDER_DAYS = 3;
    private static final List<SubscriptionStatus> LIVE = List.of(SubscriptionStatus.PENDING_PAYMENT, SubscriptionStatus.PROVISIONING,
        SubscriptionStatus.ACTIVE, SubscriptionStatus.SUSPENDED);

    private final SubscriptionRepository subscriptionRepository;
    private final MachineRepository machineRepository;
    private final CustomerService customerService;
    private final PlanService planService;
    private final InvoiceService invoiceService;
    private final MachineService machineService;
    private final MachineMaintenanceService machineMaintenanceService;
    private final RouteService routeService;
    private final PortalSettingsService portalSettingsService;
    private final SecretCipherService secretCipherService;
    private final AuditService auditService;
    private final Executor operationExecutor;
    private final int portalPort;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, MachineRepository machineRepository,
                               CustomerService customerService, PlanService planService, InvoiceService invoiceService,
                               MachineService machineService, MachineMaintenanceService machineMaintenanceService, RouteService routeService,
                               PortalSettingsService portalSettingsService, SecretCipherService secretCipherService, AuditService auditService,
                               @Qualifier("operationExecutor") Executor operationExecutor, @Value("${bancada.portal.port}") int portalPort) {
        this.subscriptionRepository = subscriptionRepository;
        this.machineRepository = machineRepository;
        this.customerService = customerService;
        this.planService = planService;
        this.invoiceService = invoiceService;
        this.machineService = machineService;
        this.machineMaintenanceService = machineMaintenanceService;
        this.routeService = routeService;
        this.portalSettingsService = portalSettingsService;
        this.secretCipherService = secretCipherService;
        this.auditService = auditService;
        this.operationExecutor = operationExecutor;
        this.portalPort = portalPort;
    }

    @Transactional(readOnly = true)
    public Page<Subscription> search(SubscriptionFilter filter, Pageable pageable) {
        Specification<Subscription> specification = Specification.where(SubscriptionSpecification.search(filter.getSearch()))
            .and(SubscriptionSpecification.customer(filter.getCustomerId()))
            .and(SubscriptionSpecification.plan(filter.getPlanId()))
            .and(SubscriptionSpecification.statusIn(filter.getStatus()));
        return subscriptionRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Subscription findById(Long id) {
        return subscriptionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Assinatura não encontrada para ID: " + id));
    }

    /** Only the owner reaches a subscription; any other id answers as not found. */
    @Transactional(readOnly = true)
    public Subscription findForCustomer(Long id, Long customerId) {
        Subscription subscription = findById(id);
        if (!subscription.getCustomer().getId().equals(customerId)) {
            throw new EntityNotFoundException("Assinatura não encontrada para ID: " + id);
        }
        return subscription;
    }

    /** New order: the subscription waits for the first invoice; the password stays encrypted until the server exists. */
    @Transactional
    public CheckoutResponse checkout(Long customerId, CheckoutRequest request) {
        Customer customer = customerService.requireActive(customerId);
        Plan plan = planService.requireForSale(request.planId());
        machineService.validateSystem(plan.getDevice(), request.distribution(), request.version());
        if (subscriptionRepository.existsByCustomerIdAndHostnameAndStatusNot(customerId, request.hostname(), SubscriptionStatus.CANCELED)) {
            throw new DataIntegrityViolationException("Você já tem um servidor chamado " + request.hostname() + ".");
        }
        Subscription subscription = new Subscription(request, customer, plan, secretCipherService.encrypt(request.password()));
        subscription.stamp(customerId);
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(AuditAction.CREATE, ENTITY, saved.getId(), null, new SubscriptionSnapshot(saved), "Contratação pelo painel");
        Invoice invoice = invoiceService.createFirst(saved);
        return new CheckoutResponse(new SubscriptionResponse(saved), new InvoiceResponse(invoice));
    }

    /**
     * First invoice paid: create the server. Renewal paid: one more period, and back on if it was
     * suspended. Runs on its own thread after the payment commits: writes made inside an
     * after-commit callback would join the finished transaction and never be saved.
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onInvoicePaid(InvoicePaidEvent event) {
        operationExecutor.execute(() -> followPayment(event.invoiceId()));
    }

    private void followPayment(Long invoiceId) {
        Invoice invoice = invoiceService.findById(invoiceId);
        Subscription subscription = findById(invoice.getSubscription().getId());
        try {
            switch (subscription.getStatus()) {
                case PENDING_PAYMENT -> provision(subscription.getId());
                case ACTIVE -> renew(subscription.getId());
                case SUSPENDED -> {
                    renew(subscription.getId());
                    if (invoiceService.oldestOpen(subscription.getId()) == null) {
                        reactivate(subscription.getId(), "Fatura paga");
                    }
                }
                default -> LOG.info("Invoice {} paid for subscription {} in status {}", invoiceId, subscription.getId(),
                    subscription.getStatus());
            }
        } catch (RuntimeException exception) {
            LOG.warn("Subscription {} could not follow the payment of invoice {}", subscription.getId(), invoiceId, exception);
        }
    }

    /**
     * Creates the machine: isolated network, three device ports of its own (SSH, 80, 443) and the
     * resources of the plan. The subscription becomes active when the machine is running.
     */
    public Subscription provision(Long id) {
        Subscription subscription = findById(id);
        SubscriptionSnapshot before = new SubscriptionSnapshot(subscription);
        if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT) {
            subscription.changeStatus(SubscriptionStatus.PROVISIONING);
            subscriptionRepository.save(subscription);
        }
        Plan plan = subscription.getPlan();
        try {
            ServerPorts ports = allocatePorts(plan.getDevice().getId(), portalSettingsService.get());
            String password = secretCipherService.decrypt(subscription.getEncryptedPendingPassword());
            MachineRequest request = new MachineRequest(plan.getDevice().getId(), machineName(subscription), subscription.getDistribution(),
                subscription.getVersion(), plan.getCpuLimit(), plan.getMemoryMb(), MachineNetworkMode.BRIDGE,
                List.of(new MachinePortRequest(ports.ssh(), SSH_PORT, "tcp"), new MachinePortRequest(ports.http(), HTTP_PORT, "tcp"),
                    new MachinePortRequest(ports.https(), HTTPS_PORT, "tcp")),
                List.of(), subscription.getUsername(), password, true, SSH_PORT, true);
            MachineCreationResponse created = machineService.create(request);
            Machine machine = machineService.findById(created.machine().id());
            subscription = findById(id);
            subscription.reserve(machine, ports.ssh(), ports.http(), ports.https(), siteHostname(subscription));
        } catch (RuntimeException exception) {
            subscription = findById(id);
            subscription.failProvisioning(exception.getMessage());
            LOG.warn("Provisioning of subscription {} failed: {}", id, exception.getMessage());
        }
        subscription.stamp(auditService.currentActorId());
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(AuditAction.PROVISION, ENTITY, id, before, new SubscriptionSnapshot(saved), saved.getProvisionError());
        return saved;
    }

    /** The machine of a subscription being created came up (or failed). */
    @EventListener
    public void onMachineStatusChanged(MachineStatusChangedEvent event) {
        Subscription subscription = subscriptionRepository.findByMachineId(event.machineId()).orElse(null);
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.PROVISIONING) {
            return;
        }
        if (event.status() == MachineStatus.FAILED) {
            subscription.failProvisioning("A criação do servidor falhou. O suporte já pode tentar de novo pelo painel administrativo.");
            subscriptionRepository.save(subscription);
            return;
        }
        if (event.status() != MachineStatus.RUNNING) {
            return;
        }
        SubscriptionSnapshot before = new SubscriptionSnapshot(subscription);
        List<String> problems = publish(subscription);
        subscription.activate(LocalDate.now());
        if (!problems.isEmpty()) {
            subscription.failProvisioning("Servidor no ar, mas: " + String.join("; ", problems));
        }
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(AuditAction.STATUS_CHANGE, ENTITY, saved.getId(), before, new SubscriptionSnapshot(saved), "Servidor criado");
    }

    /** Administrator: try the creation again (after fixing the device, for example). */
    public Subscription retryProvisioning(Long id) {
        Subscription subscription = findById(id);
        if (subscription.getStatus() != SubscriptionStatus.PROVISIONING) {
            throw new IllegalStateException("Só assinaturas sendo preparadas podem tentar de novo.");
        }
        Machine machine = subscription.getMachine();
        if (machine == null || machine.getStatus() == MachineStatus.REMOVED) {
            return provision(id);
        }
        String password = secretCipherService.decrypt(subscription.getEncryptedPendingPassword());
        if (password == null) {
            throw new IllegalStateException("A senha escolhida pelo cliente não está mais guardada; reinstale a máquina com uma senha nova.");
        }
        machineMaintenanceService.reinstall(machine.getId(),
            new MachineReinstallRequest(subscription.getDistribution(), subscription.getVersion(), password, false));
        subscription.failProvisioning(null);
        return subscriptionRepository.save(subscription);
    }

    public Subscription renew(Long id) {
        Subscription subscription = findById(id);
        SubscriptionSnapshot before = new SubscriptionSnapshot(subscription);
        subscription.extend();
        subscription.stamp(auditService.currentActorId());
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(AuditAction.UPDATE, ENTITY, id, before, new SubscriptionSnapshot(saved), "Renovação paga");
        return saved;
    }

    /** Stops the server and pauses its routes; the data stays. */
    public Subscription suspend(Long id, String reason) {
        Subscription subscription = findById(id);
        SubscriptionSnapshot before = new SubscriptionSnapshot(subscription);
        subscription.changeStatus(SubscriptionStatus.SUSPENDED);
        subscription.stamp(auditService.currentActorId());
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(AuditAction.STATUS_CHANGE, ENTITY, id, before, new SubscriptionSnapshot(saved), reason);
        enforceMachineState(saved);
        return saved;
    }

    public Subscription reactivate(Long id, String reason) {
        Subscription subscription = findById(id);
        SubscriptionSnapshot before = new SubscriptionSnapshot(subscription);
        subscription.changeStatus(SubscriptionStatus.ACTIVE);
        subscription.stamp(auditService.currentActorId());
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(AuditAction.STATUS_CHANGE, ENTITY, id, before, new SubscriptionSnapshot(saved), reason);
        enforceMachineState(saved);
        return saved;
    }

    /** Ends the subscription now: the server is removed, routes closed, open invoices canceled. */
    public Subscription cancel(Long id, String reason) {
        Subscription subscription = findById(id);
        SubscriptionSnapshot before = new SubscriptionSnapshot(subscription);
        subscription.changeStatus(SubscriptionStatus.CANCELED);
        subscription.scheduleCancel(false, reason);
        subscription.stamp(auditService.currentActorId());
        Subscription saved = subscriptionRepository.save(subscription);
        invoiceService.cancelOpen(id, "Assinatura cancelada");
        auditService.record(AuditAction.STATUS_CHANGE, ENTITY, id, before, new SubscriptionSnapshot(saved), reason);
        enforceMachineState(saved);
        return saved;
    }

    /** Customer: cancel at the end of the paid period (the usual) or right now, erasing the server. */
    public Subscription customerCancel(Long customerId, Long id, CancelSubscriptionRequest request) {
        Subscription subscription = findForCustomer(id, customerId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            throw new IllegalStateException("Esta assinatura já foi cancelada.");
        }
        boolean atPeriodEnd = request.atPeriodEnd() && subscription.getStatus() == SubscriptionStatus.ACTIVE;
        if (!atPeriodEnd) {
            return cancel(id, request.reason() == null || request.reason().isBlank() ? "Cancelada pelo cliente" : request.reason());
        }
        SubscriptionSnapshot before = new SubscriptionSnapshot(subscription);
        subscription.scheduleCancel(true, request.reason());
        subscription.stamp(customerId);
        Subscription saved = subscriptionRepository.save(subscription);
        invoiceService.cancelOpen(id, "Cancelamento agendado pelo cliente");
        auditService.record(AuditAction.UPDATE, ENTITY, id, before, new SubscriptionSnapshot(saved), "Cancelamento no fim do período");
        return saved;
    }

    public Subscription keepRenewing(Long customerId, Long id) {
        Subscription subscription = findForCustomer(id, customerId);
        if (!subscription.isCancelAtPeriodEnd() || subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Não há cancelamento agendado nesta assinatura.");
        }
        SubscriptionSnapshot before = new SubscriptionSnapshot(subscription);
        subscription.keepRenewing();
        subscription.stamp(customerId);
        Subscription saved = subscriptionRepository.save(subscription);
        auditService.record(AuditAction.UPDATE, ENTITY, id, before, new SubscriptionSnapshot(saved), "Cancelamento desfeito");
        return saved;
    }

    /** Administrator: suspend, reactivate or cancel by hand. */
    public Subscription changeStatus(Long id, SubscriptionStatus status, String reason) {
        return switch (status) {
            case SUSPENDED -> suspend(id, reason);
            case ACTIVE -> reactivate(id, reason);
            case CANCELED -> cancel(id, reason);
            default -> throw new IllegalArgumentException("O administrador só suspende, reativa ou cancela assinaturas.");
        };
    }

    /**
     * Billing round (every 10 minutes): renewal invoices, cancellations at period end, suspension and
     * cancellation for late payment, unpaid orders, and servers out of step with their subscription.
     */
    @Scheduled(initialDelay = 120_000, fixedDelay = 600_000)
    public void runBillingCycle() {
        PortalSettings settings = portalSettingsService.get();
        LocalDate today = LocalDate.now();
        for (Subscription subscription : subscriptionRepository.findByStatusIn(LIVE)) {
            try {
                billingStep(subscription, settings, today);
            } catch (RuntimeException exception) {
                LOG.warn("Billing of subscription {} failed: {}", subscription.getId(), exception.getMessage());
            }
        }
    }

    private void billingStep(Subscription subscription, PortalSettings settings, LocalDate today) {
        Long id = subscription.getId();
        Invoice open = invoiceService.oldestOpen(id);
        switch (subscription.getStatus()) {
            case PENDING_PAYMENT -> {
                if (subscription.getCreatedAt().toLocalDate().plusDays(UNPAID_ORDER_DAYS).isBefore(today)) {
                    cancel(id, "Pedido não pago em " + UNPAID_ORDER_DAYS + " dias");
                }
            }
            case ACTIVE -> {
                if (subscription.isCancelAtPeriodEnd() && !subscription.getNextDueDate().isAfter(today)) {
                    cancel(id, subscription.getCancelReason() == null ? "Fim do período, cancelamento agendado" : subscription.getCancelReason());
                } else if (open != null && open.getDueDate().plusDays(settings.getSuspendAfterDays()).isBefore(today)) {
                    suspend(id, "Fatura " + open.getId() + " vencida em " + open.getDueDate());
                } else if (open == null && !subscription.isCancelAtPeriodEnd()
                    && !subscription.getNextDueDate().minusDays(settings.getInvoiceDaysBefore()).isAfter(today)) {
                    invoiceService.createRenewal(subscription);
                } else {
                    enforceMachineState(subscription);
                }
            }
            case SUSPENDED -> {
                if (open != null && open.getDueDate().plusDays(settings.getCancelAfterDays()).isBefore(today)) {
                    cancel(id, "Fatura " + open.getId() + " vencida há mais de " + settings.getCancelAfterDays() + " dias");
                } else {
                    enforceMachineState(subscription);
                }
            }
            default -> {
                // provisioning follows the machine events
            }
        }
    }

    /**
     * Brings the machine and routes in line with the subscription: running and published when active,
     * stopped and paused when suspended, removed when canceled. A device offline is retried next round.
     */
    private void enforceMachineState(Subscription subscription) {
        Machine machine = subscription.getMachine() == null ? null : machineService.findById(subscription.getMachine().getId());
        if (machine == null || machine.getStatus() == MachineStatus.REMOVED || machine.getStatus() == MachineStatus.CREATING) {
            return;
        }
        try {
            switch (subscription.getStatus()) {
                case ACTIVE -> {
                    setRoutes(machine.getId(), RouteStatus.ACTIVE);
                    if (machine.getStatus() == MachineStatus.STOPPED) {
                        machineService.runAction(machine.getId(), MachineAction.START);
                    }
                }
                case SUSPENDED -> {
                    setRoutes(machine.getId(), RouteStatus.PAUSED);
                    if (machine.getStatus() == MachineStatus.RUNNING) {
                        machineService.runAction(machine.getId(), MachineAction.STOP);
                    }
                }
                case CANCELED -> {
                    setRoutes(machine.getId(), RouteStatus.REMOVED);
                    machineService.remove(machine.getId());
                }
                default -> {
                    // nothing to align while waiting for payment or creation
                }
            }
        } catch (RuntimeException exception) {
            LOG.info("Server of subscription {} not aligned yet: {}", subscription.getId(), exception.getMessage());
        }
    }

    private void setRoutes(Long machineId, RouteStatus target) {
        for (Route route : routeService.routesOfMachine(machineId)) {
            if (route.getStatus() != target && route.getStatus().canTransitionTo(target)) {
                routeService.changeStatus(route.getId(), target);
            }
        }
    }

    /** SSH on its public port, and the site name on HTTP and HTTPS when a domain for customer sites is set. */
    private List<String> publish(Subscription subscription) {
        List<String> problems = new ArrayList<>();
        Long machineId = subscription.getMachine().getId();
        String label = subscription.getHostname() + " (" + subscription.getCustomer().getName() + ")";
        createRoute(new RouteRequest(RouteType.TCP, null, subscription.getSshPort(), null, machineId, SSH_PORT, "SSH de " + label),
            problems::add);
        if (subscription.getSiteHostname() != null) {
            createRoute(new RouteRequest(RouteType.HTTP, subscription.getSiteHostname(), null, null, machineId, HTTP_PORT, "Site de " + label),
                problems::add);
            createRoute(new RouteRequest(RouteType.TLS, subscription.getSiteHostname(), null, null, machineId, HTTPS_PORT,
                "Site seguro de " + label), problems::add);
        }
        return problems;
    }

    private void createRoute(RouteRequest request, Consumer<String> onProblem) {
        try {
            routeService.create(request);
        } catch (RuntimeException exception) {
            onProblem.accept(exception.getMessage());
        }
    }

    /** Three ports of the customer range free on the device and on this PC (routes, gateway, panels). */
    private ServerPorts allocatePorts(Long deviceId, PortalSettings settings) {
        Set<Integer> taken = new HashSet<>(routeService.takenPublicPorts());
        taken.add(portalPort);
        for (Machine machine : machineRepository.findByDeviceIdAndStatusNot(deviceId, MachineStatus.REMOVED)) {
            for (MachinePort port : machine.getPorts()) {
                taken.add(port.getHostPort());
            }
        }
        for (Subscription subscription : subscriptionRepository.findByStatusIn(LIVE)) {
            for (Integer port : new Integer[] {subscription.getSshPort(), subscription.getHttpPort(), subscription.getHttpsPort()}) {
                if (port != null) {
                    taken.add(port);
                }
            }
        }
        List<Integer> free = new ArrayList<>();
        for (int port = settings.getPortRangeStart(); port <= settings.getPortRangeEnd() && free.size() < 3; port++) {
            if (!taken.contains(port)) {
                free.add(port);
            }
        }
        if (free.size() < 3) {
            throw new IllegalStateException("Acabaram as portas da faixa dos clientes. Aumente a faixa nas preferências do painel do cliente.");
        }
        return new ServerPorts(free.get(0), free.get(1), free.get(2));
    }

    /** Machine name on the device: customer id plus the chosen name, unique among the machines there. */
    private String machineName(Subscription subscription) {
        String base = "c" + subscription.getCustomer().getId() + "-" + subscription.getHostname();
        String name = base;
        int suffix = 2;
        Long deviceId = subscription.getPlan().getDevice().getId();
        while (machineRepository.existsByDeviceIdAndContainerNameAndStatusNot(deviceId, Machine.CONTAINER_PREFIX + name, MachineStatus.REMOVED)) {
            name = base + "-" + suffix++;
        }
        return name;
    }

    private String siteHostname(Subscription subscription) {
        String domain = portalSettingsService.get().getCustomerSitesDomain();
        if (domain == null) {
            return null;
        }
        String name = subscription.getHostname() + "." + domain;
        return routeService.isSiteNameFree(name) ? name : subscription.getHostname() + "-" + subscription.getId() + "." + domain;
    }
}
