package com.bancada.turnstile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** The check against Cloudflare siteverify: what is sent, and how each answer maps to a verdict. */
class TurnstileVerifierTest {

	private static final String URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

	private MockRestServiceServer server;
	private TurnstileVerifier verifier;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		TurnstileProperties properties = new TurnstileProperties("site-test", "secret-test", URL,
				Duration.ofSeconds(3), List.of("/login"));
		verifier = new TurnstileVerifier(properties, builder.build());
	}

	@Test
	void aBlankTokenIsMissingAndNeverCallsCloudflare() {
		assertThat(verifier.verify(null, "1.2.3.4")).isEqualTo(TurnstileVerdict.MISSING);
		assertThat(verifier.verify("  ", "1.2.3.4")).isEqualTo(TurnstileVerdict.MISSING);
		server.verify();
	}

	@Test
	void anOversizedTokenIsRejectedWithoutCallingCloudflare() {
		assertThat(verifier.verify("x".repeat(TurnstileVerifier.MAX_TOKEN_LENGTH + 1), null))
				.isEqualTo(TurnstileVerdict.REJECTED);
		server.verify();
	}

	@Test
	void aConfirmedTokenPassesAndSendsSecretResponseAndIp() {
		server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().string("secret=secret-test&response=tok&remoteip=1.2.3.4"))
				.andRespond(withSuccess("{\"success\":true,\"challenge_ts\":\"2026-10-22T10:00:00Z\",\"hostname\":\"x\"}",
						MediaType.APPLICATION_JSON));

		assertThat(verifier.verify("tok", "1.2.3.4")).isEqualTo(TurnstileVerdict.PASSED);
		server.verify();
	}

	@Test
	void aRefusedTokenIsRejected() {
		server.expect(requestTo(URL)).andRespond(withSuccess(
				"{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}", MediaType.APPLICATION_JSON));

		assertThat(verifier.verify("tok", null)).isEqualTo(TurnstileVerdict.REJECTED);
	}

	@Test
	void aWrongSecretIsUnavailableBecauseEveryLoginWouldFail() {
		server.expect(requestTo(URL)).andRespond(withSuccess(
				"{\"success\":false,\"error-codes\":[\"invalid-input-secret\"]}", MediaType.APPLICATION_JSON));

		assertThat(verifier.verify("tok", null)).isEqualTo(TurnstileVerdict.UNAVAILABLE);
	}

	@Test
	void aCloudflareServerErrorFailsClosed() {
		server.expect(requestTo(URL)).andRespond(withServerError());

		assertThat(verifier.verify("tok", null)).isEqualTo(TurnstileVerdict.UNAVAILABLE);
	}
}
