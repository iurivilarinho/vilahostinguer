package com.bancada.records;

/** Identity of an SSH server read during key exchange, before any login. */
public record HostKeyProbe(String fingerprint, String keyType) {
}
