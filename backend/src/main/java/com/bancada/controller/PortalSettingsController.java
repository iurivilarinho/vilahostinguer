package com.bancada.controller;

import com.bancada.request.PortalSettingsRequest;
import com.bancada.response.PortalSettingsResponse;
import com.bancada.service.PortalCertificateService;
import com.bancada.service.PortalSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal-settings")
@Validated
@Tag(name = "Painel do cliente — preferências", description = "Publicação, cobrança e certificado do painel do cliente")
public class PortalSettingsController {

    private final PortalSettingsService portalSettingsService;
    private final PortalCertificateService portalCertificateService;

    public PortalSettingsController(PortalSettingsService portalSettingsService, PortalCertificateService portalCertificateService) {
        this.portalSettingsService = portalSettingsService;
        this.portalCertificateService = portalCertificateService;
    }

    @Operation(summary = "Preferências atuais")
    @ApiResponse(responseCode = "200", description = "Preferências")
    @GetMapping
    public ResponseEntity<PortalSettingsResponse> get() {
        return ResponseEntity.ok(new PortalSettingsResponse(portalSettingsService.get()));
    }

    @Operation(summary = "Salva as preferências e republica o painel no gateway")
    @ApiResponse(responseCode = "200", description = "Preferências salvas")
    @PutMapping
    public ResponseEntity<PortalSettingsResponse> update(@Valid @RequestBody PortalSettingsRequest request) {
        return ResponseEntity.ok(new PortalSettingsResponse(portalSettingsService.update(request)));
    }

    @Operation(summary = "Pede o certificado HTTPS ao Let's Encrypt (em segundo plano)")
    @ApiResponse(responseCode = "200", description = "Emissão iniciada; acompanhe pela situação do certificado")
    @PostMapping("/certificate")
    public ResponseEntity<PortalSettingsResponse> issueCertificate() {
        portalCertificateService.requestIssue();
        return ResponseEntity.ok(new PortalSettingsResponse(portalSettingsService.get()));
    }
}
