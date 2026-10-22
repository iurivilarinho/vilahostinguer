package com.bancada.turnstile;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers a request the Turnstile filter refused. The filter asks the handlers in order and uses the
 * first that supports the request; the default one (last) writes Problem Details.
 */
public interface TurnstileFailureHandler {

	boolean supports(HttpServletRequest request);

	void onFailure(HttpServletRequest request, HttpServletResponse response, TurnstileVerdict verdict)
			throws IOException;
}
