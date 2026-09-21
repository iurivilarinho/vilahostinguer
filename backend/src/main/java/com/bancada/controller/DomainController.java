package com.bancada.controller;

import com.bancada.filter.DomainFilter;
import com.bancada.records.ActiveRequest;
import com.bancada.request.DomainRequest;
import com.bancada.response.DomainResponse;
import com.bancada.service.DomainService;
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
@RequestMapping("/api/domains")
@Validated
@Tag(name = "Domínios", description = "Domínios e DNS dinâmico (DDNS)")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @Operation(summary = "Lista domínios (os arquivados ficam de fora sem filtro)")
    @ApiResponse(responseCode = "200", description = "Página de domínios")
    @GetMapping
    public ResponseEntity<Page<DomainResponse>> list(DomainFilter filter, Pageable pageable) {
        return ResponseEntity.ok(domainService.search(filter, pageable).map(DomainResponse::new));
    }

    @Operation(summary = "Busca um domínio")
    @ApiResponse(responseCode = "200", description = "Domínio encontrado")
    @ApiResponse(responseCode = "404", description = "Domínio inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<DomainResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new DomainResponse(domainService.findById(id)));
    }

    @Operation(summary = "Cadastra um domínio; o token é cifrado com a conta do Windows")
    @ApiResponse(responseCode = "201", description = "Domínio cadastrado")
    @PostMapping
    public ResponseEntity<DomainResponse> create(@Valid @RequestBody DomainRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new DomainResponse(domainService.create(request)));
    }

    @Operation(summary = "Altera um domínio; token vazio mantém o atual")
    @ApiResponse(responseCode = "200", description = "Domínio alterado")
    @PutMapping("/{id}")
    public ResponseEntity<DomainResponse> update(@PathVariable Long id, @Valid @RequestBody DomainRequest request) {
        return ResponseEntity.ok(new DomainResponse(domainService.update(id, request)));
    }

    @Operation(summary = "Arquiva ou reativa um domínio")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @PatchMapping("/{id}/active")
    public ResponseEntity<DomainResponse> changeActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(new DomainResponse(domainService.changeActive(id, request.active())));
    }

    @Operation(summary = "Aponta o DNS para o IP público agora (no modo manual, só confere)")
    @ApiResponse(responseCode = "200", description = "Resultado registrado no domínio")
    @PostMapping("/{id}/sync")
    public ResponseEntity<DomainResponse> sync(@PathVariable Long id) {
        return ResponseEntity.ok(new DomainResponse(domainService.sync(id)));
    }
}
