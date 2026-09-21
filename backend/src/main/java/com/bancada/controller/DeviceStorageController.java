package com.bancada.controller;

import com.bancada.request.FormatPartitionRequest;
import com.bancada.response.OperationResponse;
import com.bancada.response.PartitionResponse;
import com.bancada.service.StorageService;
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
@RequestMapping("/api/devices/{deviceId}/partitions")
@Validated
@Tag(name = "Armazenamento", description = "Discos, partições e formatação")
public class DeviceStorageController {

    private final StorageService storageService;

    public DeviceStorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "Lista discos e partições, indicando quais podem ser formatadas")
    @ApiResponse(responseCode = "200", description = "Partições do dispositivo")
    @GetMapping
    public ResponseEntity<List<PartitionResponse>> list(@PathVariable Long deviceId) {
        return ResponseEntity.ok(storageService.partitions(deviceId));
    }

    @Operation(summary = "Formata uma partição liberada (operação em segundo plano)")
    @ApiResponse(responseCode = "201", description = "Operação iniciada")
    @ApiResponse(responseCode = "409", description = "Partição protegida ou montada")
    @PostMapping("/format")
    public ResponseEntity<OperationResponse> format(@PathVariable Long deviceId, @Valid @RequestBody FormatPartitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(storageService.format(deviceId, request)));
    }
}
