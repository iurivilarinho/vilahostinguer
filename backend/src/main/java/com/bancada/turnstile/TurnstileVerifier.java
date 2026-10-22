package com.bancada.turnstile;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Checks a Turnstile token against Cloudflare siteverify. Each token works once, so the same token
 * is never checked twice. Network failures and server-side errors are {@code UNAVAILABLE}, which the
 * filter treats as a refusal: with the check on, the system fails closed.
 */
public class TurnstileVerifier {

	private static final Logger log = LoggerFactory.getLogger(TurnstileVerifier.class);

	/** Cloudflare documents 2048 characters as the longest token. */
	static final int MAX_TOKEN_LENGTH = 2048;

	private static final List<String> CONFIG_ERRORS = List.of("missing-input-secret", "invalid-input-secret",
			"internal-error");

	private final TurnstileProperties properties;
	private final RestClient restClient;

	public TurnstileVerifier(TurnstileProperties properties, RestClient restClient) {
		this.properties = properties;
		this.restClient = restClient;
	}

	public boolean isEnabled() {
		return properties.enabled();
	}

	public TurnstileVerdict verify(String token, String remoteIp) {
		if (token == null || token.isBlank()) {
			return TurnstileVerdict.MISSING;
		}
		if (token.length() > MAX_TOKEN_LENGTH) {
			return TurnstileVerdict.REJECTED;
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", properties.secretKey());
		form.add("response", token);
		if (remoteIp != null && !remoteIp.isBlank()) {
			form.add("remoteip", remoteIp);
		}
		try {
			TurnstileSiteverifyResponse answer = restClient.post().uri(properties.verifyUrl())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve()
					.body(TurnstileSiteverifyResponse.class);
			if (answer == null) {
				return TurnstileVerdict.UNAVAILABLE;
			}
			if (answer.success()) {
				return TurnstileVerdict.PASSED;
			}
			List<String> codes = answer.errorCodes() == null ? List.of() : answer.errorCodes();
			if (codes.stream().anyMatch(CONFIG_ERRORS::contains)) {
				// the secret is wrong or Cloudflare failed: every login would be refused, so say it loudly
				log.error("Turnstile siteverify refused the configuration: {}", codes);
				return TurnstileVerdict.UNAVAILABLE;
			}
			log.info("Turnstile token refused: {}", codes);
			return TurnstileVerdict.REJECTED;
		} catch (RestClientException exception) {
			log.warn("Turnstile siteverify unreachable: {}", exception.getMessage());
			return TurnstileVerdict.UNAVAILABLE;
		}
	}
}
