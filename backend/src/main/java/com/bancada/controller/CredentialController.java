package com.bancada.controller;

import com.bancada.filter.CredentialFilter;
import com.bancada.records.ActiveRequest;
import com.bancada.request.CredentialRequest;
import com.bancada.response.CredentialResponse;
import com.bancada.response.CredentialSecretResponse;
import com.bancada.service.CredentialService;
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
@RequestMapping("/api/credentials")
@Validated
@Tag(name = "Credenciais", description = "Cofre de credenciais SSH")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Operation(summary = "Lista credenciais (sem segredos)")
    @ApiResponse(responseCode = "200", description = "Página de credenciais")
    @GetMapping
    public ResponseEntity<Page<CredentialResponse>> list(CredentialFilter filter, Pageable pageable) {
        return ResponseEntity.ok(credentialService.search(filter, pageable).map(CredentialResponse::new));
    }

    @Operation(summary = "Busca uma credencial")
    @ApiResponse(responseCode = "200", description = "Credencial encontrada")
    @ApiResponse(responseCode = "404", description = "Credencial inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<CredentialResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new CredentialResponse(credentialService.findById(id)));
    }

    @Operation(summary = "Cadastra uma credencial; o segredo é cifrado antes de ser salvo")
    @ApiResponse(responseCode = "201", description = "Credencial criada")
    @PostMapping
    public ResponseEntity<CredentialResponse> create(@Valid @RequestBody CredentialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new CredentialResponse(credentialService.create(request)));
    }

    @Operation(summary = "Altera uma credencial; segredo vazio mantém o atual")
    @ApiResponse(responseCode = "200", description = "Credencial alterada")
    @PutMapping("/{id}")
    public ResponseEntity<CredentialResponse> update(@PathVariable Long id, @Valid @RequestBody CredentialRequest request) {
        return ResponseEntity.ok(new CredentialResponse(credentialService.update(id, request)));
    }

    @Operation(summary = "Ativa ou arquiva uma credencial")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @PatchMapping("/{id}/active")
    public ResponseEntity<CredentialResponse> changeActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(new CredentialResponse(credentialService.changeActive(id, request.active())));
    }

    @Operation(summary = "Revela o segredo de uma credencial")
    @ApiResponse(responseCode = "200", description = "Segredo em texto claro")
    @PostMapping("/{id}/reveal")
    public ResponseEntity<CredentialSecretResponse> reveal(@PathVariable Long id) {
        return ResponseEntity.ok(credentialService.reveal(id));
    }
}
