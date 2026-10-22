package com.bancada.turnstile;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cloudflare Turnstile settings (PLT-033). {@code TURNSTILE_SITE_KEY} is public and goes to the
 * browser; {@code TURNSTILE_SECRET_KEY} stays on the server. The check is on only when both are set,
 * so a system without keys logs in exactly as before.
 *
 * @param siteKey public widget key
 * @param secretKey server-side secret used on siteverify
 * @param verifyUrl Cloudflare siteverify endpoint
 * @param timeout connect and read timeout of the siteverify call
 * @param protectedPaths ant patterns of the POST routes that need a token
 */
@ConfigurationProperties(prefix = "turnstile")
public record TurnstileProperties(String siteKey, String secretKey,
		@DefaultValue("https://challenges.cloudflare.com/turnstile/v0/siteverify") String verifyUrl,
		@DefaultValue("3s") Duration timeout, @DefaultValue List<String> protectedPaths) {

	public boolean enabled() {
		return hasText(siteKey) && hasText(secretKey);
	}

	/** Exactly one of the two keys set: the check stays off, and the startup log says why. */
	public boolean partiallyConfigured() {
		return hasText(siteKey) != hasText(secretKey);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
