package com.bancada.controller;

import com.bancada.models.Customer;
import com.bancada.request.CustomerRegisterRequest;
import com.bancada.request.LoginRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/auth")
@Validated
@Tag(name = "Painel do cliente — acesso", description = "Cadastro, entrada e saída do painel do cliente (sessão em cookie httpOnly)")
public class PortalAuthController {

    private final CustomerService customerService;
    private final PortalTokenService portalTokenService;
    private final AuthenticatedCustomerService authenticatedCustomerService;

    public PortalAuthController(CustomerService customerService, PortalTokenService portalTokenService,
                                AuthenticatedCustomerService authenticatedCustomerService) {
        this.customerService = customerService;
        this.portalTokenService = portalTokenService;
        this.authenticatedCustomerService = authenticatedCustomerService;
    }

    @Operation(summary = "Cria a conta e já entra")
    @ApiResponse(responseCode = "201", description = "Conta criada; cookie de sessão enviado")
    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody CustomerRegisterRequest request, HttpServletRequest http) {
        Customer customer = customerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.SET_COOKIE, portalTokenService.cookie(portalTokenService.issue(customer), http.isSecure()).toString())
            .body(new CustomerResponse(customer));
    }

    @Operation(summary = "Entra com e-mail e senha")
    @ApiResponse(responseCode = "200", description = "Sessão aberta; cookie enviado")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos")
    @ApiResponse(responseCode = "429", description = "Muitas tentativas erradas")
    @PostMapping("/login")
    public ResponseEntity<CustomerResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        Customer customer = customerService.authenticate(request, http.getRemoteAddr());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, portalTokenService.cookie(portalTokenService.issue(customer), http.isSecure()).toString())
            .body(new CustomerResponse(customer));
    }

    @Operation(summary = "Sai (apaga o cookie de sessão)")
    @ApiResponse(responseCode = "204", description = "Sessão encerrada")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, portalTokenService.clearedCookie(http.isSecure()).toString()).build();
    }

    @Operation(summary = "Cliente da sessão atual")
    @ApiResponse(responseCode = "200", description = "Cliente logado")
    @ApiResponse(responseCode = "401", description = "Sem sessão")
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> me() {
        return ResponseEntity.ok(new CustomerResponse(customerService.findById(authenticatedCustomerService.requireCustomerId())));
    }
}
