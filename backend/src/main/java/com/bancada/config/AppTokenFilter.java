package com.bancada.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards /api and /ws with the session token the Tauri shell generates at launch.
 *
 * <p>The backend holds SSH credentials and opens root shells, and it listens on a local port that
 * any process of this machine can reach. The token makes the API answer only to the window that
 * started it. Browsers cannot set headers on EventSource, WebSocket or plain downloads, so the token
 * is also accepted as the {@code token} query parameter.
 */
@Component
@Order(1)
public class AppTokenFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AppTokenFilter.class);
    public static final String TOKEN_HEADER = "X-Bancada-Token";
    private static final String TOKEN_PARAM = "token";

    private final byte[] expectedToken;

    public AppTokenFilter(@Value("${bancada.token:}") String token) {
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
        if (token.isBlank()) {
            LOG.warn("bancada.token is empty: the API is open to any local process (development mode).");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // the customer panel has its own login (JWT, see SecurityConfig)
        if (path.startsWith("/api/portal/") || path.startsWith("/ws/portal/")) {
            return true;
        }
        return expectedToken.length == 0 || !(path.startsWith("/api/") || path.startsWith("/ws/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String provided = request.getHeader(TOKEN_HEADER);
        if (provided == null || provided.isBlank()) {
            provided = request.getParameter(TOKEN_PARAM);
        }
        if (provided != null && MessageDigest.isEqual(expectedToken, provided.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now()
            + "\",\"message\":[\"Sessão inválida. Abra o painel pelo aplicativo Bancada.\"]}");
    }
}
