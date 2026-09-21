package com.bancada.controller;

import com.bancada.response.DistributionResponse;
import com.bancada.response.DockerStatusResponse;
import com.bancada.response.MachineStatsResponse;
import com.bancada.service.MachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices/{deviceId}")
@Tag(name = "Máquinas", description = "Máquinas Linux em contêineres Docker")
public class DeviceDockerController {

    private final MachineService machineService;

    public DeviceDockerController(MachineService machineService) {
        this.machineService = machineService;
    }

    @Operation(summary = "Situação do Docker e dos recursos do kernel que ele exige")
    @ApiResponse(responseCode = "200", description = "Situação do Docker")
    @GetMapping("/docker")
    public ResponseEntity<DockerStatusResponse> dockerStatus(@PathVariable Long deviceId) {
        return ResponseEntity.ok(machineService.dockerStatus(deviceId));
    }

    @Operation(summary = "Distribuições oferecidas, indicando as que têm imagem para a arquitetura do dispositivo")
    @ApiResponse(responseCode = "200", description = "Distribuições")
    @GetMapping("/machine-distributions")
    public ResponseEntity<List<DistributionResponse>> distributions(@PathVariable Long deviceId) {
        return ResponseEntity.ok(machineService.distributions(deviceId));
    }

    @Operation(summary = "Uso de CPU e memória das máquinas ligadas")
    @ApiResponse(responseCode = "200", description = "Uso por máquina")
    @GetMapping("/machine-stats")
    public ResponseEntity<List<MachineStatsResponse>> stats(@PathVariable Long deviceId) {
        return ResponseEntity.ok(machineService.stats(deviceId));
    }

    @Operation(summary = "Relê no Docker a situação real das máquinas do dispositivo")
    @ApiResponse(responseCode = "204", description = "Situações atualizadas")
    @PostMapping("/machines/sync")
    public ResponseEntity<Void> sync(@PathVariable Long deviceId) {
        machineService.syncStatuses(deviceId);
        return ResponseEntity.noContent().build();
    }
}
