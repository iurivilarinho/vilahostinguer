package com.bancada.records;

import com.bancada.enums.ConnectionType;

/**
 * An address worth probing in a discovery round. {@code connectionType} is null when the address
 * only comes from an already registered device, meaning "keep what the device has".
 */
public record DiscoveryCandidate(String host, int port, ConnectionType connectionType, String interfaceName) {
}
