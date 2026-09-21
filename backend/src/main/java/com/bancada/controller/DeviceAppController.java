package com.bancada.controller;

import com.bancada.enums.CatalogApp;
import com.bancada.records.ServiceActionRequest;
import com.bancada.response.DeviceAppResponse;
import com.bancada.response.OperationResponse;
import com.bancada.service.AppCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices/{deviceId}/apps")
@Validated
@Tag(name = "Aplicativos", description = "Catálogo de aplicativos por dispositivo")
public class DeviceAppController {

    private final AppCatalogService appCatalogService;

    public DeviceAppController(AppCatalogService appCatalogService) {
        this.appCatalogService = appCatalogService;
    }

    @Operation(summary = "Lista o catálogo com a situação de cada aplicativo no dispositivo")
    @ApiResponse(responseCode = "200", description = "Aplicativos do catálogo")
    @GetMapping
    public ResponseEntity<List<DeviceAppResponse>> list(@PathVariable Long deviceId) {
        return ResponseEntity.ok(appCatalogService.list(deviceId));
    }

    @Operation(summary = "Instala um aplicativo (operação em segundo plano)")
    @ApiResponse(responseCode = "201", description = "Operação iniciada")
    @PostMapping("/{app}/install")
    public ResponseEntity<OperationResponse> install(@PathVariable Long deviceId, @PathVariable CatalogApp app) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(appCatalogService.install(deviceId, app)));
    }

    @Operation(summary = "Remove um aplicativo (operação em segundo plano)")
    @ApiResponse(responseCode = "201", description = "Operação iniciada")
    @PostMapping("/{app}/remove")
    public ResponseEntity<OperationResponse> remove(@PathVariable Long deviceId, @PathVariable CatalogApp app) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(appCatalogService.remove(deviceId, app)));
    }

    @Operation(summary = "Inicia, para, reinicia ou ativa no boot o serviço do aplicativo")
    @ApiResponse(responseCode = "201", description = "Operação iniciada")
    @PostMapping("/{app}/service")
    public ResponseEntity<OperationResponse> serviceAction(@PathVariable Long deviceId, @PathVariable CatalogApp app,
                                                           @Valid @RequestBody ServiceActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new OperationResponse(appCatalogService.serviceAction(deviceId, app, request.action())));
    }
}
