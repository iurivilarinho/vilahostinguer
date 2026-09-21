package com.bancada.records;

import com.bancada.enums.RouteType;
import javax.net.ssl.SSLContext;

/**
 * An active route resolved to the address the gateway connects to: the device host and the port
 * that answers there (for a machine on an isolated network, the device port mapped to it).
 *
 * <p>{@code tlsContext} makes the gateway end TLS itself (the customer panel, with its Let's
 * Encrypt certificate) instead of passing the encrypted bytes on. {@code singleRequest} closes the
 * connection after one request, so every request carries fresh X-Forwarded-* headers.
 */
public record RouteTarget(Long routeId, RouteType type, String hostname, Integer publicPort, String host, int port, String label,
                          SSLContext tlsContext, boolean singleRequest) {

    public RouteTarget(Long routeId, RouteType type, String hostname, Integer publicPort, String host, int port, String label) {
        this(routeId, type, hostname, publicPort, host, port, label, null, false);
    }
}
