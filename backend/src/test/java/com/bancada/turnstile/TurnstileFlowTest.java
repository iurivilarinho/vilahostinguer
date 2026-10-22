package com.bancada.turnstile;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PLT-033: the filter in front of a real controller. Off by default the route is judged as before; on,
 * a missing token is refused and a confirmed one passes. Cloudflare itself is mocked.
 */
@ExtendWith(MockitoExtension.class)
class TurnstileFlowTest {

	@RestController
	static class LoginStub {
		@PostMapping("/login")
		String login() {
			return "reached";
		}
	}

	@Mock
	private TurnstileVerifier verifier;

	private MockMvc mvc(String siteKey, String secretKey) {
		TurnstileProperties properties = new TurnstileProperties(siteKey, secretKey, "https://example.invalid",
				Duration.ofSeconds(3), List.of("/login"));
		TurnstileFilter filter = new TurnstileFilter(verifier, properties,
				List.of(new ProblemJsonTurnstileFailureHandler()));
		return MockMvcBuilders.standaloneSetup(new LoginStub(), new TurnstileConfigController(properties))
				.addFilters(filter).build();
	}

	@BeforeEach
	void defaults() {
		when(verifier.isEnabled()).thenReturn(false);
	}

	@Test
	void offByDefaultTheLoginIsUnchangedAndTheConfigSaysDisabled() throws Exception {
		MockMvc mvc = mvc("", "");

		mvc.perform(post("/login")).andExpect(status().isOk()).andExpect(content().string("reached"));
		mvc.perform(get("/public/turnstile")).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
	}

	@Test
	void onItRefusesWithoutATokenAndPassesWithAConfirmedOne() throws Exception {
		when(verifier.isEnabled()).thenReturn(true);
		when(verifier.verify(null, "127.0.0.1")).thenReturn(TurnstileVerdict.MISSING);
		when(verifier.verify("good", "127.0.0.1")).thenReturn(TurnstileVerdict.PASSED);
		MockMvc mvc = mvc("site-test", "secret-test");

		mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("human-verification-required"));
		mvc.perform(post("/login").header("X-Turnstile-Token", "good")).andExpect(status().isOk())
				.andExpect(content().string("reached"));
		mvc.perform(get("/public/turnstile")).andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.enabled").value(true)).andExpect(jsonPath("$.siteKey").value("site-test"))
				.andExpect(jsonPath("$.secretKey").doesNotExist());
	}
}
