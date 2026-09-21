package com.bancada.controller;

import com.bancada.filter.CustomerFilter;
import com.bancada.records.CustomerStatusRequest;
import com.bancada.request.AdminPasswordResetRequest;
import com.bancada.request.CustomerProfileRequest;
import com.bancada.response.CustomerResponse;
import com.bancada.service.CustomerService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@Validated
@Tag(name = "Clientes", description = "Clientes do painel do cliente")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Lista clientes (os encerrados ficam de fora sem filtro de situação)")
    @ApiResponse(responseCode = "200", description = "Página de clientes")
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> list(CustomerFilter filter, Pageable pageable) {
        return ResponseEntity.ok(customerService.search(filter, pageable).map(CustomerResponse::new));
    }

    @Operation(summary = "Busca um cliente")
    @ApiResponse(responseCode = "200", description = "Cliente")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new CustomerResponse(customerService.findById(id)));
    }

    @Operation(summary = "Altera os dados cadastrais")
    @ApiResponse(responseCode = "200", description = "Cliente alterado")
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerProfileRequest request) {
        return ResponseEntity.ok(new CustomerResponse(customerService.updateProfile(id, request)));
    }

    @Operation(summary = "Bloqueia, desbloqueia ou encerra a conta")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @PatchMapping("/{id}/status")
    public ResponseEntity<CustomerResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody CustomerStatusRequest request) {
        return ResponseEntity.ok(new CustomerResponse(customerService.changeStatus(id, request.status(), request.reason())));
    }

    @Operation(summary = "Define uma senha nova de acesso (encerra as sessões do cliente)")
    @ApiResponse(responseCode = "200", description = "Senha definida")
    @PostMapping("/{id}/password")
    public ResponseEntity<CustomerResponse> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminPasswordResetRequest request) {
        return ResponseEntity.ok(new CustomerResponse(customerService.resetPassword(id, request.password())));
    }
}
