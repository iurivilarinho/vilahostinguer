package com.bancada.config;

import com.bancada.service.PortalCertificateService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * What answers on the customer panel port: the portal API and WebSocket, the bundles, the ACME
 * challenge and the panel page itself (portal.html for every other path). The administrator API,
 * Swagger and the administrator page do not exist there. With a certificate, plain HTTP is
 * redirected to HTTPS (except the ACME challenge, which Let's Encrypt fetches over HTTP).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortalAccessFilter extends OncePerRequestFilter {

    private static final String ACME_PATH = "/.well-known/acme-challenge/";
    private static final List<String> OPEN_PREFIXES = List.of("/api/portal/", "/ws/portal/", "/assets/", ACME_PATH);
    private static final List<String> CLOSED_PREFIXES = List.of("/api/", "/ws/", "/swagger", "/v3/");
    private static final Set<String> STATIC_FILES = Set.of("/icon.svg", "/favicon.ico", "/robots.txt");
    private static final String PAGE = "/portal.html";

    private final int portalPort;
    private final PortalCertificateService portalCertificateService;

    public PortalAccessFilter(@Value("${bancada.portal.port}") int portalPort, PortalCertificateService portalCertificateService) {
        this.portalPort = portalPort;
        this.portalCertificateService = portalCertificateService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getLocalPort() != portalPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String path = request.getRequestURI();
        String host = request.getServerName() == null ? null : request.getServerName().toLowerCase(Locale.ROOT);
        if (!path.startsWith(ACME_PATH) && !request.isSecure() && portalCertificateService.isActiveFor(host)) {
            String query = request.getQueryString();
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", "https://" + host + path + (query == null ? "" : "?" + query));
            return;
        }
        if (OPEN_PREFIXES.stream().anyMatch(path::startsWith) || STATIC_FILES.contains(path) || PAGE.equals(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (CLOSED_PREFIXES.stream().anyMatch(path::startsWith)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"message\":[\"Recurso não encontrado\"]}");
            return;
        }
        request.getRequestDispatcher(PAGE).forward(request, response);
    }
}
