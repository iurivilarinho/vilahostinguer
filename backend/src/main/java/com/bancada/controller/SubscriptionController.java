package com.bancada.controller;

import com.bancada.filter.SubscriptionFilter;
import com.bancada.records.SubscriptionStatusRequest;
import com.bancada.response.SubscriptionResponse;
import com.bancada.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@Validated
@Tag(name = "Assinaturas", description = "Servidores contratados pelos clientes")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Operation(summary = "Lista assinaturas")
    @ApiResponse(responseCode = "200", description = "Página de assinaturas")
    @GetMapping
    public ResponseEntity<Page<SubscriptionResponse>> list(SubscriptionFilter filter, Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.search(filter, pageable).map(SubscriptionResponse::new));
    }

    @Operation(summary = "Busca uma assinatura")
    @ApiResponse(responseCode = "200", description = "Assinatura")
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new SubscriptionResponse(subscriptionService.findById(id)));
    }

    @Operation(summary = "Suspende, reativa ou cancela (cancelar apaga o servidor)")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @PatchMapping("/{id}/status")
    public ResponseEntity<SubscriptionResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody SubscriptionStatusRequest request) {
        return ResponseEntity.ok(new SubscriptionResponse(subscriptionService.changeStatus(id, request.status(), request.reason())));
    }

    @Operation(summary = "Tenta criar o servidor de novo (depois de uma falha)")
    @ApiResponse(responseCode = "200", description = "Nova tentativa iniciada")
    @PostMapping("/{id}/retry")
    public ResponseEntity<SubscriptionResponse> retry(@PathVariable Long id) {
        return ResponseEntity.ok(new SubscriptionResponse(subscriptionService.retryProvisioning(id)));
    }
}
