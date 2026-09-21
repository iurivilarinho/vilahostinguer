package com.bancada.service;

import com.bancada.exception.PortalAuthenticationException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** The customer logged in the panel, as put in the security context by the portal JWT filter. */
@Service
public class AuthenticatedCustomerService {

    public Optional<Long> currentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long customerId) {
            return Optional.of(customerId);
        }
        return Optional.empty();
    }

    public Long requireCustomerId() {
        return currentCustomerId().orElseThrow(() -> new PortalAuthenticationException("Sua sessão expirou. Entre de novo."));
    }
}
