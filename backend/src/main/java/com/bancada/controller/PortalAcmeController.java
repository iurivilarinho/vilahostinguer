package com.bancada.controller;

import com.bancada.service.PortalCertificateService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Answer to the Let's Encrypt HTTP-01 challenge while the panel certificate is being issued. */
@Hidden
@RestController
public class PortalAcmeController {

    private final PortalCertificateService portalCertificateService;

    public PortalAcmeController(PortalCertificateService portalCertificateService) {
        this.portalCertificateService = portalCertificateService;
    }

    @GetMapping(value = "/.well-known/acme-challenge/{token}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> challenge(@PathVariable String token) {
        return portalCertificateService.challengeResponse(token)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
