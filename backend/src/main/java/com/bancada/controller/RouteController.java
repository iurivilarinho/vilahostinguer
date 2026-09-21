package com.bancada.controller;

import com.bancada.filter.RouteFilter;
import com.bancada.records.RouteStatusRequest;
import com.bancada.request.RouteRequest;
import com.bancada.response.RouteResponse;
import com.bancada.service.RouteService;
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
@RequestMapping("/api/routes")
@Validated
@Tag(name = "Rotas", description = "Acesso remoto a dispositivos e máquinas pelo gateway do PC")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @Operation(summary = "Lista rotas (as removidas ficam de fora sem filtro de situação)")
    @ApiResponse(responseCode = "200", description = "Página de rotas")
    @GetMapping
    public ResponseEntity<Page<RouteResponse>> list(RouteFilter filter, Pageable pageable) {
        return ResponseEntity.ok(routeService.search(filter, pageable).map(RouteResponse::new));
    }

    @Operation(summary = "Busca uma rota")
    @ApiResponse(responseCode = "200", description = "Rota encontrada")
    @ApiResponse(responseCode = "404", description = "Rota inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new RouteResponse(routeService.findById(id)));
    }

    @Operation(summary = "Cria uma rota; o gateway abre a porta na hora")
    @ApiResponse(responseCode = "201", description = "Rota criada")
    @PostMapping
    public ResponseEntity<RouteResponse> create(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new RouteResponse(routeService.create(request)));
    }

    @Operation(summary = "Altera uma rota")
    @ApiResponse(responseCode = "200", description = "Rota alterada")
    @PutMapping("/{id}")
    public ResponseEntity<RouteResponse> update(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(new RouteResponse(routeService.update(id, request)));
    }

    @Operation(summary = "Pausa, reativa ou remove uma rota")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @PatchMapping("/{id}/status")
    public ResponseEntity<RouteResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody RouteStatusRequest request) {
        return ResponseEntity.ok(new RouteResponse(routeService.changeStatus(id, request.status())));
    }
}
