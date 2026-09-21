package com.bancada.controller;

import com.bancada.filter.MachineFilter;
import com.bancada.records.MachineActionRequest;
import com.bancada.request.MachineRequest;
import com.bancada.response.MachineCreationResponse;
import com.bancada.response.MachineLogsResponse;
import com.bancada.response.MachineResponse;
import com.bancada.response.OperationResponse;
import com.bancada.service.MachineService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/machines")
@Validated
@Tag(name = "Máquinas", description = "Máquinas Linux em contêineres Docker")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @Operation(summary = "Lista máquinas (as removidas ficam de fora sem filtro de situação)")
    @ApiResponse(responseCode = "200", description = "Página de máquinas")
    @GetMapping
    public ResponseEntity<Page<MachineResponse>> list(MachineFilter filter, Pageable pageable) {
        return ResponseEntity.ok(machineService.search(filter, pageable).map(MachineResponse::new));
    }

    @Operation(summary = "Busca uma máquina")
    @ApiResponse(responseCode = "200", description = "Máquina encontrada")
    @ApiResponse(responseCode = "404", description = "Máquina inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<MachineResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new MachineResponse(machineService.findById(id)));
    }

    @Operation(summary = "Cria uma máquina: baixa a imagem, prepara o sistema, o usuário e o SSH (operação em segundo plano)")
    @ApiResponse(responseCode = "201", description = "Criação iniciada")
    @PostMapping
    public ResponseEntity<MachineCreationResponse> create(@Valid @RequestBody MachineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(machineService.create(request));
    }

    @Operation(summary = "Liga, desliga ou reinicia uma máquina")
    @ApiResponse(responseCode = "201", description = "Operação iniciada")
    @PostMapping("/{id}/action")
    public ResponseEntity<OperationResponse> action(@PathVariable Long id, @Valid @RequestBody MachineActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(machineService.runAction(id, request.action())));
    }

    @Operation(summary = "Remove a máquina do dispositivo; o registro fica no histórico")
    @ApiResponse(responseCode = "201", description = "Remoção iniciada")
    @PostMapping("/{id}/remove")
    public ResponseEntity<OperationResponse> remove(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationResponse(machineService.remove(id)));
    }

    @Operation(summary = "Últimas linhas de saída da máquina")
    @ApiResponse(responseCode = "200", description = "Saída recente")
    @GetMapping("/{id}/logs")
    public ResponseEntity<MachineLogsResponse> logs(@PathVariable Long id) {
        return ResponseEntity.ok(machineService.logs(id));
    }
}
