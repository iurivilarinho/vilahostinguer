package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancada.enums.ConnectionType;
import com.bancada.enums.DnsProvider;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineNetworkMode;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.RouteStatus;
import com.bancada.enums.RouteType;
import com.bancada.models.AppSettings;
import com.bancada.models.Device;
import com.bancada.models.Domain;
import com.bancada.models.Machine;
import com.bancada.models.Route;
import com.bancada.records.RouteTarget;
import com.bancada.records.RoutesChangedEvent;
import com.bancada.repository.DomainRepository;
import com.bancada.repository.RouteRepository;
import com.bancada.request.DomainRequest;
import com.bancada.request.MachinePortRequest;
import com.bancada.request.MachineRequest;
import com.bancada.request.RouteRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RouteServiceTest {

    private static final int PANEL_PORT = 8747;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private MachineService machineService;

    @Mock
    private SettingsService settingsService;

    @Mock
    private EventService eventService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private RouteService routeService;

    private final Device phone = new Device("169.254.1.1", 22, "SHA256:abc", "ssh-ed25519", ConnectionType.USB, null);

    @BeforeEach
    void setUp() {
        routeService = new RouteService(routeRepository, domainRepository, deviceService, machineService, settingsService, eventService,
            applicationEventPublisher, PANEL_PORT);
        when(settingsService.get()).thenReturn(new AppSettings("C:/backups"));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceService.findById(1L)).thenReturn(phone);
        when(domainRepository.findByActiveTrue()).thenReturn(List.of());
    }

    private Machine machine(MachineNetworkMode network) {
        MachineRequest request = new MachineRequest(1L, "web", MachineDistribution.DEBIAN, "12", null, null, network,
            List.of(new MachinePortRequest(8080, 80, "tcp")), List.of(), "iuri", "segredo", true, 2201, true);
        Machine machine = new Machine(request, phone);
        machine.changeStatus(MachineStatus.RUNNING);
        return machine;
    }

    @Test
    void siteRouteIsNormalizedLinkedToItsDomainAndReloadsTheGateway() {
        Domain casa = new Domain(new DomainRequest("casa.duckdns.org", DnsProvider.DUCKDNS, "t", false, true, 30), "casa.duckdns.org", "x");
        Domain duck = new Domain(new DomainRequest("duckdns.org", DnsProvider.MANUAL, null, false, false, 30), "duckdns.org", null);
        when(domainRepository.findByActiveTrue()).thenReturn(List.of(duck, casa));

        Route route = routeService.create(new RouteRequest(RouteType.HTTP, " Blog.Casa.DuckDNS.org. ", null, 1L, null, 80, null));

        assertEquals("blog.casa.duckdns.org", route.getHostname());
        assertEquals(casa, route.getDomain(), "the most specific domain wins");
        verify(applicationEventPublisher).publishEvent(any(RoutesChangedEvent.class));
    }

    @Test
    void tcpRouteCannotTakeThePanelOrTheSharedSitePorts() {
        assertThrows(IllegalArgumentException.class,
            () -> routeService.create(new RouteRequest(RouteType.TCP, null, PANEL_PORT, 1L, null, 22, null)));
        assertThrows(IllegalArgumentException.class,
            () -> routeService.create(new RouteRequest(RouteType.TCP, null, 80, 1L, null, 22, null)));
        verify(routeRepository, never()).save(any());
    }

    @Test
    void duplicatedPublicPortIsRejected() {
        when(routeRepository.existsByTypeAndPublicPortAndStatusNotAndIdNot(eq(RouteType.TCP), eq(2201), eq(RouteStatus.REMOVED), anyLong()))
            .thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
            () -> routeService.create(new RouteRequest(RouteType.TCP, null, 2201, 1L, null, 22, null)));
    }

    @Test
    void isolatedMachineOnlyExposesMappedPortsAndResolvesToTheDevicePort() {
        Machine isolated = machine(MachineNetworkMode.BRIDGE);
        when(machineService.findById(3L)).thenReturn(isolated);

        assertThrows(IllegalArgumentException.class,
            () -> routeService.create(new RouteRequest(RouteType.HTTP, "web.exemplo.com", null, null, 3L, 443, null)));

        Route route = routeService.create(new RouteRequest(RouteType.HTTP, "web.exemplo.com", null, null, 3L, 80, null));
        when(routeRepository.findByStatus(RouteStatus.ACTIVE)).thenReturn(List.of(route));
        List<RouteTarget> targets = routeService.activeTargets();

        assertEquals(phone, route.getDevice());
        assertEquals(1, targets.size());
        assertEquals("169.254.1.1", targets.get(0).host());
        assertEquals(8080, targets.get(0).port(), "container port 80 answers on device port 8080");
    }

    @Test
    void routesToRemovedMachinesAreLeftOutOfTheGateway() {
        Machine hostNetwork = machine(MachineNetworkMode.HOST);
        when(machineService.findById(3L)).thenReturn(hostNetwork);
        Route route = routeService.create(new RouteRequest(RouteType.TCP, null, 2201, null, 3L, 2201, "SSH"));
        hostNetwork.changeStatus(MachineStatus.REMOVED);
        when(routeRepository.findByStatus(RouteStatus.ACTIVE)).thenReturn(List.of(route));

        assertTrue(routeService.activeTargets().isEmpty());
    }

    @Test
    void removedRouteCannotBeReactivated() {
        Route route = routeService.create(new RouteRequest(RouteType.TCP, null, 2222, 1L, null, 22, null));
        route.changeStatus(RouteStatus.REMOVED);
        when(routeRepository.findById(9L)).thenReturn(java.util.Optional.of(route));

        assertThrows(IllegalStateException.class, () -> routeService.changeStatus(9L, RouteStatus.ACTIVE));
        ArgumentCaptor<RoutesChangedEvent> event = ArgumentCaptor.forClass(RoutesChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(event.capture());
    }
}
