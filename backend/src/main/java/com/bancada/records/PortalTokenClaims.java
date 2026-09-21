package com.bancada.records;

/** What a valid customer panel token says: who, and which session generation it belongs to. */
public record PortalTokenClaims(Long customerId, int tokenVersion) {
}
