package com.bancada.turnstile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** The filter guards only the configured POST routes, and only while the check is on. */
@ExtendWith(MockitoExtension.class)
class TurnstileFilterTest {

	@Mock
	private TurnstileVerifier verifier;

	private TurnstileFilter filter;
	private MockFilterChain chain;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		TurnstileProperties properties = new TurnstileProperties("site", "secret", "https://example.invalid",
				Duration.ofSeconds(3), List.of("/login", "/v1/accounts", "/v1/accounts/password-reset"));
		filter = new TurnstileFilter(verifier, properties, List.of(new ProblemJsonTurnstileFailureHandler()));
		chain = new MockFilterChain();
		response = new MockHttpServletResponse();
	}

	private MockHttpServletRequest post(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
		request.setRemoteAddr("9.9.9.9");
		return request;
	}

	@Test
	void withTheCheckOffEveryRequestPassesUntouched() throws Exception {
		when(verifier.isEnabled()).thenReturn(false);

		filter.doFilter(post("/login"), response, chain);

		assertThat(chain.getRequest()).isNotNull();
		verify(verifier, never()).verify(any(), any());
	}

	@Test
	void unprotectedRoutesAndGetRequestsPass() throws Exception {
		when(verifier.isEnabled()).thenReturn(true);

		filter.doFilter(post("/v1/me"), response, chain);
		assertThat(chain.getRequest()).isNotNull();

		MockFilterChain getChain = new MockFilterChain();
		filter.doFilter(new MockHttpServletRequest("GET", "/login"), response, getChain);
		assertThat(getChain.getRequest()).isNotNull();
		verify(verifier, never()).verify(any(), any());
	}

	@Test
	void aTokenInTheHeaderIsCheckedWithTheClientIpAndLetsTheRequestThrough() throws Exception {
		when(verifier.isEnabled()).thenReturn(true);
		when(verifier.verify("tok", "9.9.9.9")).thenReturn(TurnstileVerdict.PASSED);
		MockHttpServletRequest request = post("/v1/accounts");
		request.addHeader(TurnstileFilter.TOKEN_HEADER, "tok");

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void aTokenInTheFormFieldIsAccepted() throws Exception {
		when(verifier.isEnabled()).thenReturn(true);
		when(verifier.verify("formtok", "9.9.9.9")).thenReturn(TurnstileVerdict.PASSED);
		MockHttpServletRequest request = post("/login");
		request.setParameter(TurnstileFilter.TOKEN_PARAMETER, "formtok");

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
	}

	@Test
	void aMissingTokenIsAnswered400AndStopsTheChain() throws Exception {
		when(verifier.isEnabled()).thenReturn(true);
		when(verifier.verify(null, "9.9.9.9")).thenReturn(TurnstileVerdict.MISSING);

		filter.doFilter(post("/v1/accounts/password-reset"), response, chain);

		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(response.getContentAsString()).contains("\"code\":\"human-verification-required\"")
				.contains("Conclua a verificação de segurança");
	}

	@Test
	void aRefusedTokenIs400AndAnUnreachableCloudflareIs503() throws Exception {
		when(verifier.isEnabled()).thenReturn(true);
		when(verifier.verify("bad", "9.9.9.9")).thenReturn(TurnstileVerdict.REJECTED);
		MockHttpServletRequest refused = post("/login");
		refused.addHeader(TurnstileFilter.TOKEN_HEADER, "bad");
		filter.doFilter(refused, response, chain);
		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getContentAsString()).contains("human-verification-failed");

		when(verifier.verify("late", "9.9.9.9")).thenReturn(TurnstileVerdict.UNAVAILABLE);
		MockHttpServletRequest down = post("/login");
		down.addHeader(TurnstileFilter.TOKEN_HEADER, "late");
		MockHttpServletResponse downResponse = new MockHttpServletResponse();
		filter.doFilter(down, downResponse, new MockFilterChain());
		assertThat(downResponse.getStatus()).isEqualTo(503);
		assertThat(downResponse.getContentAsString()).contains("human-verification-unavailable");
	}
}
