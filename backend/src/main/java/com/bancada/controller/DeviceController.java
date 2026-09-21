package com.bancada.controller;

import com.bancada.filter.DeviceFilter;
import com.bancada.records.ActiveRequest;
import com.bancada.request.DeviceRequest;
import com.bancada.response.DeviceMetricsResponse;
import com.bancada.response.DeviceResponse;
import com.bancada.response.OperationResponse;
import com.bancada.service.AppCatalogService;
import com.bancada.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
@Validated
@Tag(name = "Dispositivos", description = "Dispositivos detectados e cadastrados")
public class DeviceController {

    private final DeviceService deviceService;
    private final AppCatalogService appCatalogService;

    public DeviceController(DeviceService deviceService, AppCatalogService appCatalogService) {
        this.deviceService = deviceService;
        this.appCatalogService = appCatalogService;
    }

    @Operation(summary = "Lista dispositivos")
    @ApiResponse(responseCode = "200", description = "Página de dispositivos")
    @GetMapping
    public ResponseEntity<Page<DeviceResponse>> list(DeviceFilter filter, Pageable pageable) {
        return ResponseEntity.ok(deviceService.search(filter, pageable).map(DeviceResponse::new));
    }

    @Operation(summary = "Busca um dispositivo")
    @ApiResponse(responseCode = "200", description = "Dispositivo encontrado")
    @ApiResponse(responseCode = "404", description = "Dispositivo inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new DeviceResponse(deviceService.findById(id)));
    }

    @Operation(summary = "Cadastra um dispositivo pelo endereço (lê a chave do servidor SSH para identificá-lo)")
    @ApiResponse(responseCode = "201", description = "Dispositivo cadastrado")
    @ApiResponse(responseCode = "502", description = "Nenhum servidor SSH respondeu no endereço")
    @PostMapping
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody DeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new DeviceResponse(deviceService.createManual(request)));
    }

    @Operation(summary = "Altera nome, endereço, credencial e anotações")
    @ApiResponse(responseCode = "200", description = "Dispositivo alterado")
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> update(@PathVariable Long id, @Valid @RequestBody DeviceRequest request) {
        return ResponseEntity.ok(new DeviceResponse(deviceService.update(id, request)));
    }

    @Operation(summary = "Ativa ou arquiva um dispositivo")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @PatchMapping("/{id}/active")
    public ResponseEntity<DeviceResponse> changeActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(new DeviceResponse(deviceService.changeActive(id, request.active())));
    }

    @Operation(summary = "Entra no dispositivo e relê as informações do sistema")
    @ApiResponse(responseCode = "200", description = "Informações atualizadas")
    @ApiResponse(responseCode = "502", description = "Dispositivo inacessível ou credencial recusada")
    @PostMapping("/{id}/facts")
    public ResponseEntity<DeviceResponse> refreshFacts(@PathVariable Long id) {
        return ResponseEntity.ok(new DeviceResponse(deviceService.refreshFacts(id)));
    }

    @Operation(summary = "Lê o uso de CPU, memória, disco, bateria e temperatura")
    @ApiResponse(responseCode = "200", description = "Uso atual de recursos")
    @GetMapping("/{id}/metrics")
    public ResponseEntity<DeviceMetricsResponse> metrics(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.metrics(id));
    }

    @Operation(summary = "Atualiza os pacotes do sistema (operação em segundo plano)")
    @ApiResponse(responseCode = "201", description = "Operação iniciada")
    @PostMapping("/{id}/upgrade")
    public ResponseEntity<OperationResponse> upgrade(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(appCatalogService.upgradeSystem(id)));
    }
}
