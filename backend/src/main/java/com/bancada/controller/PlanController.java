package com.bancada.controller;

import com.bancada.filter.PlanFilter;
import com.bancada.request.PlanRequest;
import com.bancada.response.PlanResponse;
import com.bancada.service.PlanService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@Validated
@Tag(name = "Planos", description = "Planos de servidor à venda no painel do cliente")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Lista planos, com estoque e quantas assinaturas cada um tem")
    @ApiResponse(responseCode = "200", description = "Página de planos")
    @GetMapping
    public ResponseEntity<Page<PlanResponse>> list(PlanFilter filter, Pageable pageable) {
        return ResponseEntity.ok(planService.search(filter, pageable).map(planService::toResponse));
    }

    @Operation(summary = "Busca um plano")
    @ApiResponse(responseCode = "200", description = "Plano")
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(planService.toResponse(planService.findById(id)));
    }

    @Operation(summary = "Cria um plano")
    @ApiResponse(responseCode = "201", description = "Plano criado")
    @PostMapping
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.toResponse(planService.create(request)));
    }

    @Operation(summary = "Altera um plano (quem já contratou mantém o preço da contratação)")
    @ApiResponse(responseCode = "200", description = "Plano alterado")
    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> update(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.toResponse(planService.update(id, request)));
    }
}
