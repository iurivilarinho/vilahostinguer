package com.bancada.controller;

import com.bancada.request.GatewaySettingsRequest;
import com.bancada.response.GatewayStatusResponse;
import com.bancada.service.GatewayService;
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
@RequestMapping("/api/gateway")
@Validated
@Tag(name = "Gateway", description = "Portas abertas no PC, IP público, UPnP e tráfego do acesso remoto")
public class GatewayController {

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Operation(summary = "Situação do gateway: portas, IP público, CGNAT, UPnP e tráfego por rota")
    @ApiResponse(responseCode = "200", description = "Situação atual")
    @GetMapping("/status")
    public ResponseEntity<GatewayStatusResponse> status() {
        return ResponseEntity.ok(gatewayService.status());
    }

    @Operation(summary = "Altera as preferências do gateway e reabre as portas")
    @ApiResponse(responseCode = "200", description = "Preferências salvas; situação atualizada")
    @PutMapping("/settings")
    public ResponseEntity<GatewayStatusResponse> updateSettings(@Valid @RequestBody GatewaySettingsRequest request) {
        return ResponseEntity.ok(gatewayService.updateSettings(request));
    }

    @Operation(summary = "Procura o roteador de novo e reaplica as portas por UPnP")
    @ApiResponse(responseCode = "200", description = "Situação atualizada")
    @PostMapping("/upnp/refresh")
    public ResponseEntity<GatewayStatusResponse> refreshUpnp() {
        return ResponseEntity.ok(gatewayService.refreshUpnp());
    }
}
