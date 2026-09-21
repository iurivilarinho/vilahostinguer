package com.bancada.controller;

import com.bancada.request.SettingsRequest;
import com.bancada.response.SettingsResponse;
import com.bancada.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@Validated
@Tag(name = "Configurações", description = "Preferências do painel")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Operation(summary = "Lê as preferências")
    @ApiResponse(responseCode = "200", description = "Preferências atuais")
    @GetMapping
    public ResponseEntity<SettingsResponse> get() {
        return ResponseEntity.ok(new SettingsResponse(settingsService.get()));
    }

    @Operation(summary = "Salva as preferências")
    @ApiResponse(responseCode = "200", description = "Preferências salvas")
    @PutMapping
    public ResponseEntity<SettingsResponse> update(@Valid @RequestBody SettingsRequest request) {
        return ResponseEntity.ok(new SettingsResponse(settingsService.update(request)));
    }
}
