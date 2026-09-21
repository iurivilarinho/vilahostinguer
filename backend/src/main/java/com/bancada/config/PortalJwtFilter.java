package com.bancada.config;

import com.bancada.enums.CustomerStatus;
import com.bancada.models.Customer;
import com.bancada.records.PortalTokenClaims;
import com.bancada.repository.CustomerRepository;
import com.bancada.service.PortalTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates customer panel requests from the JWT in the {@code token} cookie (or a Bearer
 * header). A token of an older session generation (password changed, account closed) is ignored,
 * and the request goes on anonymous: the security chain answers 401 where a login is required.
 */
public class PortalJwtFilter extends OncePerRequestFilter {

    public static final String CUSTOMER_ROLE = "CUSTOMER";
    private static final String BEARER = "Bearer ";

    private final PortalTokenService portalTokenService;
    private final CustomerRepository customerRepository;

    public PortalJwtFilter(PortalTokenService portalTokenService, CustomerRepository customerRepository) {
        this.portalTokenService = portalTokenService;
        this.customerRepository = customerRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/portal/") || path.startsWith("/ws/portal/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        portalTokenService.parse(token(request))
            .flatMap(this::activeSession)
            .ifPresent(customer -> SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                customer.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + CUSTOMER_ROLE)))));
        chain.doFilter(request, response);
    }

    private Optional<Customer> activeSession(PortalTokenClaims claims) {
        return customerRepository.findById(claims.customerId())
            .filter(customer -> customer.getTokenVersion() == claims.tokenVersion())
            .filter(customer -> customer.getStatus() != CustomerStatus.CLOSED);
    }

    private static String token(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (PortalTokenService.COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith(BEARER) ? header.substring(BEARER.length()) : null;
    }
}
