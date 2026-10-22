package com.bancada.turnstile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Default answer: Problem Details (RFC 9457) with 400, or 503 when Cloudflare is out of reach. The
 * body is written by hand so the class works whichever JSON library the system uses.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ProblemJsonTurnstileFailureHandler implements TurnstileFailureHandler {

	@Override
	public boolean supports(HttpServletRequest request) {
		return true;
	}

	@Override
	public void onFailure(HttpServletRequest request, HttpServletResponse response, TurnstileVerdict verdict)
			throws IOException {
		HttpStatus status = verdict == TurnstileVerdict.UNAVAILABLE ? HttpStatus.SERVICE_UNAVAILABLE
				: HttpStatus.BAD_REQUEST;
		String body = "{\"type\":\"/problems/" + verdict.getCode() + "\",\"title\":\"Verificação de segurança\","
				+ "\"status\":" + status.value() + ",\"detail\":\"" + verdict.getMessage() + "\",\"code\":\""
				+ verdict.getCode() + "\"}";
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(body);
	}
}
