package com.bancada.records;

import com.bancada.enums.RouteType;

/**
 * An active route resolved to the address the gateway connects to: the device host and the port
 * that answers there (for a machine on an isolated network, the device port mapped to it).
 */
public record RouteTarget(Long routeId, RouteType type, String hostname, Integer publicPort, String host, int port, String label) {
}
