package com.bancada.records;

/** Outcome of pushing the public IP to a DNS provider; zoneId is the Cloudflare zone, cached on the domain. */
public record DdnsUpdateResult(String message, String zoneId) {
}
