package com.bancada.controller;

import com.bancada.filter.OperationFilter;
import com.bancada.response.OperationBasicResponse;
import com.bancada.response.OperationResponse;
import com.bancada.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
@Validated
@Tag(name = "Atividades", description = "Histórico de operações nos dispositivos")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @Operation(summary = "Lista operações")
    @ApiResponse(responseCode = "200", description = "Página de operações")
    @GetMapping
    public ResponseEntity<Page<OperationBasicResponse>> list(OperationFilter filter, Pageable pageable) {
        return ResponseEntity.ok(operationService.search(filter, pageable).map(OperationBasicResponse::new));
    }

    @Operation(summary = "Busca uma operação com a saída completa")
    @ApiResponse(responseCode = "200", description = "Operação encontrada")
    @ApiResponse(responseCode = "404", description = "Operação inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<OperationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new OperationResponse(operationService.findById(id)));
    }

    @Operation(summary = "Cancela uma operação em andamento")
    @ApiResponse(responseCode = "200", description = "Cancelamento pedido")
    @ApiResponse(responseCode = "409", description = "A operação já terminou")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OperationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(new OperationResponse(operationService.cancel(id)));
    }
}
