package com.bancada.controller;

import com.bancada.response.ScanStatusResponse;
import com.bancada.service.DiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery")
@Tag(name = "Detecção", description = "Busca automática de dispositivos")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Operation(summary = "Situação da última busca")
    @ApiResponse(responseCode = "200", description = "Adaptadores vistos e endereços verificados")
    @GetMapping("/status")
    public ResponseEntity<ScanStatusResponse> status() {
        return ResponseEntity.ok(discoveryService.status());
    }

    @Operation(summary = "Procura dispositivos agora")
    @ApiResponse(responseCode = "200", description = "Resultado da busca")
    @PostMapping("/scan")
    public ResponseEntity<ScanStatusResponse> scan() {
        return ResponseEntity.ok(discoveryService.scanNow());
    }
}
