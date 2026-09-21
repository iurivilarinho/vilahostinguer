package com.bancada.service;

import com.bancada.records.UpnpGateway;
import com.bancada.response.UpnpStatusResponse;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Opens the gateway ports on the home router through UPnP IGD, the same protocol game consoles use:
 * SSDP multicast finds the router, a SOAP call maps each port to this PC. Routers with UPnP turned
 * off (common, and safer) do not answer; the status page then explains the manual port forward.
 */
@Service
public class UpnpService {

    private static final Logger LOG = LoggerFactory.getLogger(UpnpService.class);
    private static final String SSDP_ADDRESS = "239.255.255.250";
    private static final int SSDP_PORT = 1900;
    private static final int DISCOVERY_WAIT_MS = 3_000;
    private static final int SOCKET_TIMEOUT_MS = 500;
    private static final int CONNECT_TIMEOUT_MS = 2_000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration GATEWAY_CACHE = Duration.ofMinutes(10);
    private static final Duration MAPPING_REFRESH = Duration.ofMinutes(20);
    private static final int FALLBACK_LEASE_SECONDS = 86_400;
    private static final String PROTOCOL = "TCP";
    private static final List<String> SEARCH_TARGETS = List.of(
        "urn:schemas-upnp-org:device:InternetGatewayDevice:1",
        "urn:schemas-upnp-org:device:InternetGatewayDevice:2",
        "urn:schemas-upnp-org:service:WANIPConnection:1",
        "urn:schemas-upnp-org:service:WANPPPConnection:1");
    private static final Pattern LOCATION = Pattern.compile("(?im)^location:\\s*(\\S+)\\s*$");
    private static final Pattern SERVICE = Pattern.compile("<service>(.*?)</service>", Pattern.DOTALL);

    private final HttpClient httpClient;
    private final Set<Integer> mappedPorts = ConcurrentHashMap.newKeySet();
    private volatile UpnpGateway gateway;
    private volatile Instant gatewayFoundAt;
    private volatile Instant mappingsRefreshedAt;
    private volatile boolean enabled;
    private volatile String externalIp;
    private volatile String error;
    private volatile LocalDateTime checkedAt;

    public UpnpService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /** Leaves the router with exactly these ports mapped to this PC (none when disabled). */
    public synchronized void sync(Set<Integer> ports, boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            removeAll();
            error = null;
            return;
        }
        try {
            UpnpGateway current = gateway();
            externalIp = externalIp(current);
            boolean refresh = mappingsRefreshedAt == null || mappingsRefreshedAt.plus(MAPPING_REFRESH).isBefore(Instant.now());
            for (int port : ports) {
                if (refresh || !mappedPorts.contains(port)) {
                    addMapping(current, port);
                    mappedPorts.add(port);
                }
            }
            for (Integer port : Set.copyOf(mappedPorts)) {
                if (!ports.contains(port)) {
                    deleteMapping(current, port);
                    mappedPorts.remove(port);
                }
            }
            if (refresh) {
                mappingsRefreshedAt = Instant.now();
            }
            error = null;
        } catch (IOException | IllegalStateException exception) {
            error = exception.getMessage();
            gateway = null;
            LOG.info("UPnP: {}", error);
        } finally {
            checkedAt = LocalDateTime.now();
        }
    }

    /** Removes every mapping this panel created (gateway off, UPnP off, panel closing). */
    public synchronized void removeAll() {
        if (mappedPorts.isEmpty() || gateway == null) {
            mappedPorts.clear();
            return;
        }
        for (Integer port : Set.copyOf(mappedPorts)) {
            try {
                deleteMapping(gateway, port);
            } catch (IOException | IllegalStateException exception) {
                LOG.info("UPnP: could not remove port {}: {}", port, exception.getMessage());
            }
            mappedPorts.remove(port);
        }
    }

    /** Forgets the router found before, so the next sync searches again. */
    public void forgetGateway() {
        gateway = null;
        mappingsRefreshedAt = null;
    }

    public String externalIp() {
        return externalIp;
    }

    public UpnpStatusResponse status() {
        UpnpGateway current = gateway;
        return new UpnpStatusResponse(enabled, current != null, current == null ? null : current.name(),
            current == null ? null : current.localAddress(), externalIp, List.copyOf(new TreeSet<>(mappedPorts)), error, checkedAt);
    }

    private UpnpGateway gateway() throws IOException {
        UpnpGateway current = gateway;
        if (current != null && gatewayFoundAt.plus(GATEWAY_CACHE).isAfter(Instant.now())) {
            return current;
        }
        UpnpGateway found = discover();
        if (current == null || !current.controlUrl().equals(found.controlUrl())) {
            mappedPorts.clear();
            mappingsRefreshedAt = null;
        }
        gateway = found;
        gatewayFoundAt = Instant.now();
        return found;
    }

    private UpnpGateway discover() throws IOException {
        Set<String> locations = new LinkedHashSet<>();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            InetAddress group = InetAddress.getByName(SSDP_ADDRESS);
            for (String target : SEARCH_TARGETS) {
                byte[] search = ("M-SEARCH * HTTP/1.1\r\nHOST: " + SSDP_ADDRESS + ":" + SSDP_PORT + "\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: "
                    + target + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
                socket.send(new DatagramPacket(search, search.length, group, SSDP_PORT));
            }
            long deadline = System.currentTimeMillis() + DISCOVERY_WAIT_MS;
            byte[] buffer = new byte[2048];
            while (System.currentTimeMillis() < deadline) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException timeout) {
                    continue;
                }
                Matcher matcher = LOCATION.matcher(new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII));
                if (matcher.find()) {
                    locations.add(matcher.group(1));
                }
            }
        }
        for (String location : locations) {
            try {
                UpnpGateway found = describe(location);
                if (found != null) {
                    return found;
                }
            } catch (IOException | IllegalArgumentException exception) {
                LOG.debug("UPnP: {} ignored: {}", location, exception.getMessage());
            }
        }
        throw new IllegalStateException(locations.isEmpty()
            ? "Nenhum roteador respondeu ao UPnP. Ligue o UPnP no roteador ou faça o redirecionamento de portas à mão."
            : "Há aparelhos UPnP na rede, mas nenhum oferece redirecionamento de portas.");
    }

    private UpnpGateway describe(String location) throws IOException {
        String xml = get(location);
        Matcher services = SERVICE.matcher(xml);
        while (services.find()) {
            String block = services.group(1);
            String type = tag(block, "serviceType");
            String control = tag(block, "controlURL");
            if (type == null || control == null || !(type.contains(":WANIPConnection:") || type.contains(":WANPPPConnection:"))) {
                continue;
            }
            String base = tag(xml, "URLBase");
            URI controlUri = URI.create(base != null && !base.isBlank() ? base : location).resolve(control);
            URI locationUri = URI.create(location);
            String localAddress;
            try (Socket probe = new Socket()) {
                probe.connect(new InetSocketAddress(locationUri.getHost(), locationUri.getPort() < 0 ? 80 : locationUri.getPort()),
                    CONNECT_TIMEOUT_MS);
                localAddress = probe.getLocalAddress().getHostAddress();
            }
            String name = tag(xml, "friendlyName");
            String model = tag(xml, "modelName");
            return new UpnpGateway(location, controlUri.toString(), type, localAddress,
                name != null ? name : model != null ? model : locationUri.getHost());
        }
        return null;
    }

    private String externalIp(UpnpGateway current) throws IOException {
        String response = soap(current, "GetExternalIPAddress", "");
        return tag(response, "NewExternalIPAddress");
    }

    private void addMapping(UpnpGateway current, int port) throws IOException {
        try {
            soap(current, "AddPortMapping", mappingArguments(current, port, 0));
        } catch (IllegalStateException permanentRefused) {
            // some routers only accept leases with an expiry; the periodic refresh renews them
            soap(current, "AddPortMapping", mappingArguments(current, port, FALLBACK_LEASE_SECONDS));
        }
        LOG.info("UPnP: port {} mapped to {}", port, current.localAddress());
    }

    private void deleteMapping(UpnpGateway current, int port) throws IOException {
        soap(current, "DeletePortMapping", "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port
            + "</NewExternalPort><NewProtocol>" + PROTOCOL + "</NewProtocol>");
        LOG.info("UPnP: port {} unmapped", port);
    }

    private static String mappingArguments(UpnpGateway current, int port, int leaseSeconds) {
        return "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port + "</NewExternalPort><NewProtocol>" + PROTOCOL
            + "</NewProtocol><NewInternalPort>" + port + "</NewInternalPort><NewInternalClient>" + current.localAddress()
            + "</NewInternalClient><NewEnabled>1</NewEnabled><NewPortMappingDescription>Bancada " + port
            + "</NewPortMappingDescription><NewLeaseDuration>" + leaseSeconds + "</NewLeaseDuration>";
    }

    private String soap(UpnpGateway current, String action, String arguments) throws IOException {
        String envelope = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:" + action + " xmlns:u=\""
            + current.serviceType() + "\">" + arguments + "</u:" + action + "></s:Body></s:Envelope>";
        HttpRequest request = HttpRequest.newBuilder(URI.create(current.controlUrl()))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "text/xml; charset=\"utf-8\"")
            .header("SOAPAction", "\"" + current.serviceType() + "#" + action + "\"")
            .POST(HttpRequest.BodyPublishers.ofString(envelope))
            .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            String description = tag(response.body(), "errorDescription");
            String code = tag(response.body(), "errorCode");
            throw new IllegalStateException("O roteador recusou " + action + ": "
                + (description != null ? description : "HTTP " + response.statusCode()) + (code != null ? " (" + code + ")" : ""));
        }
        return response.body();
    }

    private String get(String url) throws IOException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(url)).timeout(REQUEST_TIMEOUT).GET().build());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("interrompido", exception);
        }
    }

    /** Content of the first element with this local name, ignoring any namespace prefix. */
    static String tag(String xml, String name) {
        if (xml == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("<(?:[A-Za-z0-9_]+:)?" + name + "(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z0-9_]+:)?" + name + ">",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(xml);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}
