package com.bancada.turnstile;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Requires a valid Turnstile token on the configured public POST routes (login, sign-up, password
 * reset). The token comes in the {@code X-Turnstile-Token} header (JSON calls) or in the
 * {@code cf-turnstile-response} field the widget adds to a form. With the check off the filter does
 * nothing. It is added inside the security chains, after CORS, never as a plain servlet filter.
 */
public class TurnstileFilter extends OncePerRequestFilter {

	public static final String TOKEN_HEADER = "X-Turnstile-Token";
	public static final String TOKEN_PARAMETER = "cf-turnstile-response";

	private final TurnstileVerifier verifier;
	private final TurnstileProperties properties;
	private final List<TurnstileFailureHandler> failureHandlers;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public TurnstileFilter(TurnstileVerifier verifier, TurnstileProperties properties,
			List<TurnstileFailureHandler> failureHandlers) {
		this.verifier = verifier;
		this.properties = properties;
		this.failureHandlers = failureHandlers;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!verifier.isEnabled() || !HttpMethod.POST.matches(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return properties.protectedPaths().stream().noneMatch(pattern -> pathMatcher.match(pattern, path));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = request.getHeader(TOKEN_HEADER);
		if (token == null || token.isBlank()) {
			token = request.getParameter(TOKEN_PARAMETER);
		}
		TurnstileVerdict verdict = verifier.verify(token, request.getRemoteAddr());
		if (verdict == TurnstileVerdict.PASSED) {
			chain.doFilter(request, response);
			return;
		}
		for (TurnstileFailureHandler handler : failureHandlers) {
			if (handler.supports(request)) {
				handler.onFailure(request, response, verdict);
				return;
			}
		}
	}
}
