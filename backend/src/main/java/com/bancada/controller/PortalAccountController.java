package com.bancada.controller;

import com.bancada.models.Customer;
import com.bancada.request.ChangePasswordRequest;
import com.bancada.request.CustomerProfileRequest;
import com.bancada.response.CustomerResponse;
import com.bancada.service.AuthenticatedCustomerService;
import com.bancada.service.CustomerService;
import com.bancada.service.PortalTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/account")
@Validated
@Tag(name = "Painel do cliente — conta", description = "Dados cadastrais e senha de acesso")
public class PortalAccountController {

    private final CustomerService customerService;
    private final AuthenticatedCustomerService authenticatedCustomerService;
    private final PortalTokenService portalTokenService;

    public PortalAccountController(CustomerService customerService, AuthenticatedCustomerService authenticatedCustomerService,
                                   PortalTokenService portalTokenService) {
        this.customerService = customerService;
        this.authenticatedCustomerService = authenticatedCustomerService;
        this.portalTokenService = portalTokenService;
    }

    @Operation(summary = "Altera nome, e-mail, telefone e documento")
    @ApiResponse(responseCode = "200", description = "Dados salvos")
    @PutMapping
    public ResponseEntity<CustomerResponse> update(@Valid @RequestBody CustomerProfileRequest request) {
        return ResponseEntity.ok(new CustomerResponse(customerService.updateProfile(authenticatedCustomerService.requireCustomerId(), request)));
    }

    @Operation(summary = "Troca a senha; as outras sessões são encerradas e esta recebe um cookie novo")
    @ApiResponse(responseCode = "200", description = "Senha trocada")
    @PostMapping("/password")
    public ResponseEntity<CustomerResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest http) {
        Customer customer = customerService.changePassword(authenticatedCustomerService.requireCustomerId(), request);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, portalTokenService.cookie(portalTokenService.issue(customer), http.isSecure()).toString())
            .body(new CustomerResponse(customer));
    }
}
