package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancada.enums.BillingCycle;
import com.bancada.enums.ConnectionType;
import com.bancada.enums.MachineAction;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineNetworkMode;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.RouteType;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.models.Customer;
import com.bancada.models.Device;
import com.bancada.models.Invoice;
import com.bancada.models.Machine;
import com.bancada.models.Plan;
import com.bancada.models.PortalSettings;
import com.bancada.models.Subscription;
import com.bancada.records.DeviceFacts;
import com.bancada.records.MachineStatusChangedEvent;
import com.bancada.repository.MachineRepository;
import com.bancada.repository.SubscriptionRepository;
import com.bancada.request.CheckoutRequest;
import com.bancada.request.CustomerRegisterRequest;
import com.bancada.request.MachinePortRequest;
import com.bancada.request.MachineRequest;
import com.bancada.request.PlanRequest;
import com.bancada.request.RouteRequest;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTest {

    private static final int PORTAL_PORT = 8748;

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private MachineRepository machineRepository;
    @Mock private CustomerService customerService;
    @Mock private PlanService planService;
    @Mock private InvoiceService invoiceService;
    @Mock private MachineService machineService;
    @Mock private MachineMaintenanceService machineMaintenanceService;
    @Mock private RouteService routeService;
    @Mock private PortalSettingsService portalSettingsService;
    @Mock private SecretCipherService secretCipherService;
    @Mock private AuditService auditService;

    private SubscriptionService subscriptionService;
    private final Device phone = new Device("169.254.1.1", 22, "SHA256:abc", "ssh-ed25519", ConnectionType.USB, null);
    private Customer customer;
    private Plan plan;
    private PortalSettings settings;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        Executor direct = Runnable::run;
        subscriptionService = new SubscriptionService(subscriptionRepository, machineRepository, customerService, planService, invoiceService,
            machineService, machineMaintenanceService, routeService, portalSettingsService, secretCipherService, auditService, direct, PORTAL_PORT);
        phone.applyFacts(new DeviceFacts("j4", "postmarketOS", "edge", "3.18.140", "aarch64", null, 4, null, null, null, null,
            "apk", "openrc", "/root", true));
        customer = withId(new Customer(new CustomerRegisterRequest("Maria Souza", "maria@teste.com", "senha-forte", null, null, true), "hash"), 7L);
        plan = withId(new Plan(new PlanRequest("VPS 1", null, 1L, 1.0, 256, 5, 2, new BigDecimal("10.00"), true, false, 1), phone), 3L);
        settings = new PortalSettings();
        when(customerService.requireActive(7L)).thenReturn(customer);
        when(planService.requireForSale(3L)).thenReturn(plan);
        when(portalSettingsService.get()).thenReturn(settings);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(secretCipherService.encrypt(anyString())).thenAnswer(invocation -> "cifrado:" + invocation.getArgument(0));
        when(secretCipherService.decrypt(anyString())).thenAnswer(invocation -> ((String) invocation.getArgument(0)).substring(8));
        when(invoiceService.createFirst(any(Subscription.class))).thenAnswer(invocation ->
            new Invoice(invocation.getArgument(0), "primeira", false, LocalDate.now()));
        when(routeService.takenPublicPorts()).thenReturn(Set.of(80, 443, 8747, 20000));
    }

    private static <T> T withId(T entity, Long id) throws ReflectiveOperationException {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
        return entity;
    }

    private CheckoutRequest checkout(String hostname) {
        return new CheckoutRequest(3L, BillingCycle.ANNUAL, hostname, MachineDistribution.UBUNTU, "24.04", "maria", "senha-servidor");
    }

    private Subscription paidSubscription() throws ReflectiveOperationException {
        Subscription subscription = withId(new Subscription(checkout("meu-site"), customer, plan, "cifrado:senha-servidor"), 11L);
        when(subscriptionRepository.findById(11L)).thenReturn(Optional.of(subscription));
        return subscription;
    }

    @Test
    void checkoutFixesTheAnnualPriceWithDiscountAndKeepsThePasswordEncrypted() {
        subscriptionService.checkout(7L, checkout("meu-site"));
        Subscription subscription = captureSaved();

        assertEquals(new BigDecimal("96.00"), subscription.getPrice(), "12 x 10.00 with 20% off");
        assertEquals("cifrado:senha-servidor", subscription.getEncryptedPendingPassword());
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, subscription.getStatus());
        verify(invoiceService).createFirst(subscription);
    }

    private Subscription captureSaved() {
        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void sameServerNameTwiceForTheSameCustomerIsRefused() {
        when(subscriptionRepository.existsByCustomerIdAndHostnameAndStatusNot(7L, "meu-site", SubscriptionStatus.CANCELED)).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> subscriptionService.checkout(7L, checkout("meu-site")));
        verify(invoiceService, never()).createFirst(any());
    }

    @Test
    void provisioningCreatesAnIsolatedMachineWithThreeFreePortsOfTheRange() throws ReflectiveOperationException {
        Subscription subscription = paidSubscription();
        Machine machine = withId(new Machine(new MachineRequest(1L, "c7-meu-site", MachineDistribution.UBUNTU, "24.04", 1.0, 256,
            MachineNetworkMode.BRIDGE, List.of(), List.of(), "maria", "x", true, 22, true), phone), 40L);
        when(machineService.create(any(MachineRequest.class))).thenReturn(new com.bancada.response.MachineCreationResponse(
            new com.bancada.response.MachineResponse(machine), 99L));
        when(machineService.findById(40L)).thenReturn(machine);

        subscriptionService.provision(11L);

        ArgumentCaptor<MachineRequest> request = ArgumentCaptor.forClass(MachineRequest.class);
        verify(machineService).create(request.capture());
        assertEquals(MachineNetworkMode.BRIDGE, request.getValue().networkMode());
        assertEquals("c7-meu-site", request.getValue().name());
        assertEquals("senha-servidor", request.getValue().password());
        List<Integer> hostPorts = request.getValue().ports().stream().map(MachinePortRequest::hostPort).toList();
        assertEquals(List.of(20001, 20002, 20003), hostPorts, "20000 is taken by a route");
        assertEquals(List.of(22, 80, 443), request.getValue().ports().stream().map(MachinePortRequest::containerPort).toList());
        assertEquals(SubscriptionStatus.PROVISIONING, subscription.getStatus());
        assertEquals(20001, subscription.getSshPort());
    }

    @Test
    void runningMachineActivatesTheSubscriptionPublishesSshAndForgetsThePassword() throws ReflectiveOperationException {
        Subscription subscription = paidSubscription();
        subscription.changeStatus(SubscriptionStatus.PROVISIONING);
        Machine machine = withId(new Machine(), 40L);
        subscription.reserve(machine, 20001, 20002, 20003, null);
        when(subscriptionRepository.findByMachineId(40L)).thenReturn(Optional.of(subscription));

        subscriptionService.onMachineStatusChanged(new MachineStatusChangedEvent(40L, MachineStatus.RUNNING));

        ArgumentCaptor<RouteRequest> route = ArgumentCaptor.forClass(RouteRequest.class);
        verify(routeService).create(route.capture());
        assertEquals(RouteType.TCP, route.getValue().type());
        assertEquals(20001, route.getValue().publicPort());
        assertEquals(22, route.getValue().targetPort());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(LocalDate.now().plusMonths(12), subscription.getNextDueDate());
        assertNull(subscription.getEncryptedPendingPassword());
    }

    @Test
    void lateInvoiceSuspendsTheServerAfterTheGraceDays() throws ReflectiveOperationException {
        Subscription subscription = paidSubscription();
        subscription.changeStatus(SubscriptionStatus.PROVISIONING);
        Machine machine = withId(new Machine(new MachineRequest(1L, "c7-meu-site", MachineDistribution.UBUNTU, "24.04", 1.0, 256,
            MachineNetworkMode.BRIDGE, List.of(), List.of(), "maria", "x", true, 22, true), phone), 40L);
        machine.changeStatus(MachineStatus.RUNNING);
        subscription.reserve(machine, 20001, 20002, 20003, null);
        subscription.activate(LocalDate.now().minusMonths(12));
        Invoice late = new Invoice(subscription, "renovação", true, LocalDate.now().minusDays(settings.getSuspendAfterDays() + 1));
        when(subscriptionRepository.findByStatusIn(any())).thenReturn(List.of(subscription));
        when(invoiceService.oldestOpen(11L)).thenReturn(late);
        when(machineService.findById(40L)).thenReturn(machine);
        when(routeService.routesOfMachine(anyLong())).thenReturn(List.of());

        subscriptionService.runBillingCycle();

        assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
        verify(machineService).runAction(40L, MachineAction.STOP);
    }

    @Test
    void renewalInvoiceIsIssuedDaysBeforeTheDueDate() throws ReflectiveOperationException {
        Subscription subscription = paidSubscription();
        subscription.changeStatus(SubscriptionStatus.PROVISIONING);
        subscription.activate(LocalDate.now().minusMonths(12).plusDays(settings.getInvoiceDaysBefore() - 1));
        when(subscriptionRepository.findByStatusIn(any())).thenReturn(List.of(subscription));
        when(invoiceService.oldestOpen(11L)).thenReturn(null);

        subscriptionService.runBillingCycle();

        verify(invoiceService).createRenewal(subscription);
    }

    @Test
    void unpaidOrderIsCanceledAfterThreeDays() throws ReflectiveOperationException {
        Subscription subscription = paidSubscription();
        Field createdAt = Subscription.class.getDeclaredField("createdAt");
        createdAt.setAccessible(true);
        createdAt.set(subscription, java.time.LocalDateTime.now().minusDays(4));
        when(subscriptionRepository.findByStatusIn(any())).thenReturn(List.of(subscription));

        subscriptionService.runBillingCycle();

        assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
        verify(invoiceService).cancelOpen(eq(11L), anyString());
        assertTrue(subscription.getCanceledAt() != null);
    }
}
