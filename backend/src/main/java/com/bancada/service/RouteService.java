package com.bancada.service;

import com.bancada.enums.DeviceEventType;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.RouteStatus;
import com.bancada.enums.RouteType;
import com.bancada.filter.RouteFilter;
import com.bancada.models.AppSettings;
import com.bancada.models.Device;
import com.bancada.models.Domain;
import com.bancada.models.Machine;
import com.bancada.models.Route;
import com.bancada.records.RouteTarget;
import com.bancada.records.RoutesChangedEvent;
import com.bancada.repository.DomainRepository;
import com.bancada.repository.RouteRepository;
import com.bancada.request.RouteRequest;
import com.bancada.specification.RouteSpecification;
import com.bancada.validation.Hostnames;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Remote access routes and their resolution to the address the gateway connects to. */
@Service
public class RouteService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final DomainRepository domainRepository;
    private final DeviceService deviceService;
    private final MachineService machineService;
    private final SettingsService settingsService;
    private final EventService eventService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final int panelPort;

    public RouteService(RouteRepository routeRepository, DomainRepository domainRepository, DeviceService deviceService,
                        MachineService machineService, SettingsService settingsService, EventService eventService,
                        ApplicationEventPublisher applicationEventPublisher, @Value("${server.port}") int panelPort) {
        this.routeRepository = routeRepository;
        this.domainRepository = domainRepository;
        this.deviceService = deviceService;
        this.machineService = machineService;
        this.settingsService = settingsService;
        this.eventService = eventService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.panelPort = panelPort;
    }

    @Transactional(readOnly = true)
    public Page<Route> search(RouteFilter filter, Pageable pageable) {
        Specification<Route> specification = Specification.where(RouteSpecification.search(filter.getSearch()))
            .and(RouteSpecification.typeIn(filter.getType()))
            .and(RouteSpecification.statusIn(filter.getStatus()))
            .and(RouteSpecification.device(filter.getDeviceId()))
            .and(RouteSpecification.machine(filter.getMachineId()))
            .and(RouteSpecification.domain(filter.getDomainId()));
        return routeRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Route findById(Long id) {
        return routeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Rota não encontrada para ID: " + id));
    }

    @Transactional
    public Route create(RouteRequest request) {
        String hostname = validate(request, -1L);
        Machine machine = request.machineId() == null ? null : machineService.findById(request.machineId());
        Device device = targetDevice(request, machine);
        Route route = routeRepository.save(new Route(request, hostname, device, machine, coveringDomain(hostname)));
        changed("Rota " + describe(route) + " criada");
        return route;
    }

    @Transactional
    public Route update(Long id, RouteRequest request) {
        Route route = findById(id);
        if (route.getStatus() == RouteStatus.REMOVED) {
            throw new IllegalStateException("A rota foi removida.");
        }
        String hostname = validate(request, id);
        Machine machine = request.machineId() == null ? null : machineService.findById(request.machineId());
        Device device = targetDevice(request, machine);
        route.update(request, hostname, device, machine, coveringDomain(hostname));
        Route saved = routeRepository.save(route);
        changed("Rota " + describe(saved) + " alterada");
        return saved;
    }

    @Transactional
    public Route changeStatus(Long id, RouteStatus status) {
        Route route = findById(id);
        if (status == RouteStatus.ACTIVE) {
            ensureEntryFree(route.getType(), route.getHostname(), route.getPublicPort(), id);
        }
        route.changeStatus(status);
        Route saved = routeRepository.save(route);
        changed("Rota " + describe(saved) + ": " + status.getDescription().toLowerCase());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Route> routesOfMachine(Long machineId) {
        return routeRepository.findByMachineIdAndStatusIn(machineId, List.of(RouteStatus.ACTIVE, RouteStatus.PAUSED));
    }

    /** Public ports taken by TCP routes, plus the shared site ports and the panel port. */
    @Transactional(readOnly = true)
    public Set<Integer> takenPublicPorts() {
        Set<Integer> taken = new HashSet<>();
        routeRepository.findByStatusNot(RouteStatus.REMOVED).stream()
            .filter(route -> route.getPublicPort() != null)
            .forEach(route -> taken.add(route.getPublicPort()));
        AppSettings settings = settingsService.get();
        taken.add(settings.getGatewayHttpPort());
        taken.add(settings.getGatewayTlsPort());
        taken.add(panelPort);
        return taken;
    }

    @Transactional(readOnly = true)
    public boolean isSiteNameFree(String hostname) {
        return !routeRepository.existsByTypeAndHostnameAndStatusNotAndIdNot(RouteType.HTTP, hostname, RouteStatus.REMOVED, -1L)
            && !routeRepository.existsByTypeAndHostnameAndStatusNotAndIdNot(RouteType.TLS, hostname, RouteStatus.REMOVED, -1L);
    }

    /** Rejects gateway ports already taken by a TCP route (or by the panel itself). */
    @Transactional(readOnly = true)
    public void ensureGatewayPortsFree(int httpPort, int tlsPort) {
        if (httpPort == tlsPort) {
            throw new IllegalArgumentException("As portas HTTP e HTTPS precisam ser diferentes.");
        }
        for (int port : List.of(httpPort, tlsPort)) {
            if (port == panelPort) {
                throw new IllegalArgumentException("A porta " + port + " é a do próprio painel.");
            }
            if (routeRepository.existsByTypeAndPublicPortAndStatusNotAndIdNot(RouteType.TCP, port, RouteStatus.REMOVED, -1L)) {
                throw new DataIntegrityViolationException("A porta " + port + " já é usada por uma rota TCP.");
            }
        }
    }

    /** Re-links every site route to the most specific active domain that covers its name. */
    @Transactional
    public void linkDomains() {
        List<Domain> domains = domainRepository.findByActiveTrue();
        for (Route route : routeRepository.findByStatusNot(RouteStatus.REMOVED)) {
            if (!route.getType().isNameBased()) {
                continue;
            }
            Domain best = domains.stream()
                .filter(domain -> domain.covers(route.getHostname()))
                .max(Comparator.comparingInt(domain -> domain.getName().length()))
                .orElse(null);
            if (route.linkDomain(best)) {
                routeRepository.save(route);
            }
        }
    }

    /** Active routes whose destination exists, resolved to device address and port. */
    @Transactional(readOnly = true)
    public List<RouteTarget> activeTargets() {
        List<RouteTarget> targets = new ArrayList<>();
        for (Route route : routeRepository.findByStatus(RouteStatus.ACTIVE)) {
            Device device = route.getDevice();
            Machine machine = route.getMachine();
            if (!device.isActive() || (machine != null && machine.getStatus() == MachineStatus.REMOVED)) {
                continue;
            }
            String label = machine != null ? "A máquina " + machine.getName() : "O dispositivo " + device.getName();
            targets.add(new RouteTarget(route.getId(), route.getType(), route.getHostname(), route.getPublicPort(), device.getHost(),
                route.getTargetPort(), label));
        }
        return targets;
    }

    private String validate(RouteRequest request, Long id) {
        String hostname = null;
        if (request.type().isNameBased()) {
            hostname = Hostnames.normalize(request.hostname());
            if (!Hostnames.isValid(hostname)) {
                throw new IllegalArgumentException("Informe o nome do site, como blog.casa.duckdns.org.");
            }
        } else if (request.publicPort() == null) {
            throw new IllegalArgumentException("Informe a porta que será aberta no PC.");
        } else {
            AppSettings settings = settingsService.get();
            int port = request.publicPort();
            if (port == panelPort) {
                throw new IllegalArgumentException("A porta " + port + " é a do próprio painel.");
            }
            if (port == settings.getGatewayHttpPort() || port == settings.getGatewayTlsPort()) {
                throw new IllegalArgumentException("A porta " + port + " é a porta compartilhada dos sites; escolha outra.");
            }
        }
        ensureEntryFree(request.type(), hostname, request.publicPort(), id);
        return hostname;
    }

    private void ensureEntryFree(RouteType type, String hostname, Integer publicPort, Long id) {
        if (type.isNameBased() && routeRepository.existsByTypeAndHostnameAndStatusNotAndIdNot(type, hostname, RouteStatus.REMOVED, id)) {
            throw new DataIntegrityViolationException("Já existe uma rota " + type.getDescription() + " para " + hostname + ".");
        }
        if (!type.isNameBased() && routeRepository.existsByTypeAndPublicPortAndStatusNotAndIdNot(type, publicPort, RouteStatus.REMOVED, id)) {
            throw new DataIntegrityViolationException("A porta " + publicPort + " já é usada por outra rota.");
        }
    }

    private Device targetDevice(RouteRequest request, Machine machine) {
        if (machine == null) {
            if (request.deviceId() == null) {
                throw new IllegalArgumentException("Escolha o dispositivo ou a máquina de destino.");
            }
            return deviceService.findById(request.deviceId());
        }
        if (machine.getStatus() == MachineStatus.REMOVED) {
            throw new IllegalStateException("A máquina " + machine.getName() + " foi removida.");
        }
        // a machine is a virtual machine with an address of its own: the route goes straight to it
        return machine.getDevice();
    }

    private Domain coveringDomain(String hostname) {
        if (hostname == null) {
            return null;
        }
        return domainRepository.findByActiveTrue().stream()
            .filter(domain -> domain.covers(hostname))
            .max(Comparator.comparingInt(domain -> domain.getName().length()))
            .orElse(null);
    }

    private void changed(String message) {
        applicationEventPublisher.publishEvent(new RoutesChangedEvent(message));
        eventService.publish(DeviceEventType.NETWORK_UPDATED, null, null, null, message);
    }

    private static String describe(Route route) {
        return route.getType().isNameBased() ? route.getHostname() : "da porta " + route.getPublicPort();
    }
}
