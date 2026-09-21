package com.bancada.records;

/** Published after a route or the gateway preferences change, so the gateway reopens its ports. */
public record RoutesChangedEvent(String reason) {
}
