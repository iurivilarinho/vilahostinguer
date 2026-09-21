package com.bancada.service;

import com.bancada.enums.DeviceEventType;
import com.bancada.enums.RouteType;
import com.bancada.gateway.GatewayListener;
import com.bancada.gateway.TrafficCounter;
import com.bancada.models.AppSettings;
import com.bancada.models.PortalSettings;
import com.bancada.records.RouteTarget;
import com.bancada.records.RoutesChangedEvent;
import com.bancada.request.GatewaySettingsRequest;
import com.bancada.response.GatewayListenerResponse;
import com.bancada.response.GatewayStatusResponse;
import com.bancada.response.RouteTrafficResponse;
import com.bancada.response.UpnpStatusResponse;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Remote access gateway: keeps open on this PC the ports the active routes need (a shared one for
 * HTTP sites, a shared one for HTTPS sites, one per TCP route) and relays each connection to the
 * device or machine behind it. The PC is the only door: devices on USB or link-local networks are
 * unreachable from the internet on their own.
 */
@Service
public class GatewayService {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayService.class);
    /** Traffic of the customer panel is counted under this id (no route row). */
    public static final Long PORTAL_ROUTE_ID = 0L;
    private static final String LOOPBACK = "127.0.0.1";
    private static final String PORTAL_LABEL = "O painel do cliente";

    private final RouteService routeService;
    private final SettingsService settingsService;
    private final UpnpService upnpService;
    private final PublicIpService publicIpService;
    private final EventService eventService;
    private final PortalSettingsService portalSettingsService;
    private final PortalCertificateService portalCertificateService;
    private final int portalPort;
    private final ExecutorService executor = Executors.newCachedThreadPool(daemonThreads());
    private final Map<Integer, GatewayListener> listeners = new HashMap<>();
    private final Map<Integer, String> listenerErrors = new ConcurrentHashMap<>();
    private final Map<Long, TrafficCounter> traffic = new ConcurrentHashMap<>();
    private volatile Map<Integer, RouteType> wantedPorts = Map.of();
    private volatile Map<Integer, Integer> routesPerPort = Map.of();

    public GatewayService(RouteService routeService, SettingsService settingsService, UpnpService upnpService,
                          PublicIpService publicIpService, EventService eventService, PortalSettingsService portalSettingsService,
                          PortalCertificateService portalCertificateService, @Value("${bancada.portal.port}") int portalPort) {
        this.routeService = routeService;
        this.settingsService = settingsService;
        this.upnpService = upnpService;
        this.publicIpService = publicIpService;
        this.eventService = eventService;
        this.portalSettingsService = portalSettingsService;
        this.portalCertificateService = portalCertificateService;
        this.portalPort = portalPort;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        reload();
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onRoutesChanged(RoutesChangedEvent event) {
        reload();
    }

    /** Device addresses and machines change on their own; the table is rebuilt every minute. */
    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void periodicReload() {
        reload();
    }

    public synchronized void reload() {
        AppSettings settings = settingsService.get();
        List<RouteTarget> targets = settings.isGatewayEnabled() ? routeService.activeTargets() : List.of();
        Map<String, RouteTarget> http = new HashMap<>();
        Map<String, RouteTarget> tls = new HashMap<>();
        Map<Integer, RouteTarget> tcp = new HashMap<>();
        for (RouteTarget target : targets) {
            switch (target.type()) {
                case HTTP -> http.put(target.hostname(), target);
                case TLS -> tls.put(target.hostname(), target);
                case TCP -> tcp.put(target.publicPort(), target);
            }
        }
        PortalSettings portal = portalSettingsService.get();
        if (settings.isGatewayEnabled() && portal.isPublished()) {
            String name = portal.getHostname();
            http.put(name, new RouteTarget(PORTAL_ROUTE_ID, RouteType.HTTP, name, null, LOOPBACK, portalPort, PORTAL_LABEL, null, true));
            SSLContext certificate = portalCertificateService.contextFor(name);
            if (certificate != null) {
                tls.put(name, new RouteTarget(PORTAL_ROUTE_ID, RouteType.TLS, name, null, LOOPBACK, portalPort, PORTAL_LABEL, certificate, true));
            }
        }
        Map<Integer, RouteType> wanted = new LinkedHashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        if (!http.isEmpty()) {
            wanted.put(settings.getGatewayHttpPort(), RouteType.HTTP);
            counts.put(settings.getGatewayHttpPort(), http.size());
        }
        if (!tls.isEmpty()) {
            wanted.put(settings.getGatewayTlsPort(), RouteType.TLS);
            counts.put(settings.getGatewayTlsPort(), tls.size());
        }
        tcp.keySet().forEach(port -> {
            wanted.putIfAbsent(port, RouteType.TCP);
            counts.putIfAbsent(port, 1);
        });

        listeners.entrySet().removeIf(entry -> {
            if (wanted.get(entry.getKey()) == entry.getValue().getType()) {
                return false;
            }
            entry.getValue().close();
            return true;
        });
        listenerErrors.keySet().retainAll(wanted.keySet());
        for (Map.Entry<Integer, RouteType> entry : wanted.entrySet()) {
            int port = entry.getKey();
            Function<String, RouteTarget> resolver = switch (entry.getValue()) {
                case HTTP -> Map.copyOf(http)::get;
                case TLS -> Map.copyOf(tls)::get;
                case TCP -> {
                    RouteTarget target = tcp.get(port);
                    yield name -> target;
                }
            };
            GatewayListener existing = listeners.get(port);
            if (existing != null) {
                existing.updateResolver(resolver);
                continue;
            }
            try {
                listeners.put(port, GatewayListener.open(port, entry.getValue(), executor, resolver, this::counter));
                listenerErrors.remove(port);
            } catch (BindException exception) {
                listenerErrors.put(port, "A porta " + port + " já está em uso por outro programa deste PC.");
            } catch (IOException exception) {
                listenerErrors.put(port, "Não foi possível abrir a porta " + port + ": " + exception.getMessage());
            }
        }
        wantedPorts = Collections.unmodifiableMap(wanted);
        routesPerPort = Map.copyOf(counts);

        Set<Integer> openPorts = Set.copyOf(listeners.keySet());
        boolean upnp = settings.isGatewayEnabled() && settings.isUpnpEnabled();
        executor.execute(() -> upnpService.sync(openPorts, upnp));
    }

    public GatewayStatusResponse updateSettings(GatewaySettingsRequest request) {
        routeService.ensureGatewayPortsFree(request.httpPort(), request.tlsPort());
        settingsService.updateGateway(request);
        upnpService.forgetGateway();
        reload();
        eventService.publish(DeviceEventType.NETWORK_UPDATED, null, null, null, "Preferências do acesso remoto alteradas");
        return status();
    }

    /** Searches the router again and reapplies the port mappings (after turning UPnP on in the router). */
    public GatewayStatusResponse refreshUpnp() {
        AppSettings settings = settingsService.get();
        upnpService.forgetGateway();
        Set<Integer> openPorts;
        synchronized (this) {
            openPorts = Set.copyOf(listeners.keySet());
        }
        upnpService.sync(openPorts, settings.isGatewayEnabled() && settings.isUpnpEnabled());
        return status();
    }

    public GatewayStatusResponse status() {
        AppSettings settings = settingsService.get();
        if (publicIpService.isStale()) {
            executor.execute(() -> {
                try {
                    publicIpService.current();
                } catch (IllegalStateException ignored) {
                    // kept in lastError
                }
            });
        }
        String publicIp = publicIpService.lastKnown();
        String publicIpError = publicIpService.lastError();
        List<GatewayListenerResponse> ports = new ArrayList<>();
        Map<Integer, Integer> counts = routesPerPort;
        Set<Integer> listening;
        synchronized (this) {
            listening = Set.copyOf(listeners.keySet());
        }
        wantedPorts.forEach((port, type) -> ports.add(new GatewayListenerResponse(port, type, type.getDescription(),
            listening.contains(port), listenerErrors.get(port), counts.getOrDefault(port, 0))));
        List<RouteTrafficResponse> routeTraffic = traffic.entrySet().stream()
            .map(entry -> entry.getValue().toResponse(entry.getKey()))
            .toList();
        UpnpStatusResponse upnp = upnpService.status();
        String cgnatReason = cgnatReason(publicIp, upnp.externalIp());
        return new GatewayStatusResponse(settings.isGatewayEnabled(), settings.getGatewayHttpPort(), settings.getGatewayTlsPort(),
            publicIp, publicIpError, lanAddresses(), cgnatReason != null, cgnatReason, ports, routeTraffic, upnp);
    }

    @PreDestroy
    public synchronized void shutdown() {
        listeners.values().forEach(GatewayListener::close);
        listeners.clear();
        upnpService.removeAll();
        executor.shutdownNow();
    }

    private TrafficCounter counter(Long routeId) {
        return traffic.computeIfAbsent(routeId, id -> new TrafficCounter());
    }

    /**
     * The router gets its WAN address from the carrier; when that address is private (or differs
     * from what the internet sees) there is another NAT in front and no port forward can help.
     */
    static String cgnatReason(String publicIp, String routerIp) {
        if (routerIp == null || routerIp.isBlank()) {
            return null;
        }
        if (isSharedOrPrivate(routerIp)) {
            return "O roteador recebe da operadora o endereço " + routerIp + ", que é interno (CGNAT). Conexões de fora não chegam "
                + "até ele: peça IP público à operadora ou use um túnel (Cloudflare Tunnel, Tailscale).";
        }
        if (publicIp != null && !publicIp.equals(routerIp)) {
            return "O roteador tem o endereço " + routerIp + ", mas a internet vê " + publicIp + ": há outro roteador ou CGNAT no "
                + "caminho. O redirecionamento de portas precisa ser feito no aparelho da frente.";
        }
        return null;
    }

    static boolean isSharedOrPrivate(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        return first == 10 || (first == 100 && second >= 64 && second <= 127) || (first == 172 && second >= 16 && second <= 31)
            || (first == 192 && second == 168) || first == 0;
    }

    /** Private IPv4 addresses of this PC (link-local USB networks left out), for the manual port forward. */
    private static List<String> lanAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException exception) {
            LOG.debug("Could not list network interfaces: {}", exception.getMessage());
        }
        return addresses;
    }

    private static ThreadFactory daemonThreads() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "gateway-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
