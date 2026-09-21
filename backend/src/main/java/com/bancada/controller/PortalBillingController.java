package com.bancada.controller;

import com.bancada.filter.InvoiceFilter;
import com.bancada.filter.SubscriptionFilter;
import com.bancada.request.CancelSubscriptionRequest;
import com.bancada.request.CheckoutRequest;
import com.bancada.response.CheckoutResponse;
import com.bancada.response.InvoiceResponse;
import com.bancada.response.SubscriptionResponse;
import com.bancada.service.AuthenticatedCustomerService;
import com.bancada.service.InvoiceService;
import com.bancada.service.SubscriptionService;
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
@RequestMapping("/api/portal")
@Validated
@Tag(name = "Painel do cliente — faturamento", description = "Contratação, assinaturas e faturas do cliente logado")
public class PortalBillingController {

    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final AuthenticatedCustomerService authenticatedCustomerService;

    public PortalBillingController(SubscriptionService subscriptionService, InvoiceService invoiceService,
                                   AuthenticatedCustomerService authenticatedCustomerService) {
        this.subscriptionService = subscriptionService;
        this.invoiceService = invoiceService;
        this.authenticatedCustomerService = authenticatedCustomerService;
    }

    @Operation(summary = "Contrata um servidor: cria a assinatura e a primeira fatura")
    @ApiResponse(responseCode = "201", description = "Pedido criado; pague a fatura para o servidor ser criado")
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(subscriptionService.checkout(authenticatedCustomerService.requireCustomerId(), request));
    }

    @Operation(summary = "Assinaturas do cliente")
    @ApiResponse(responseCode = "200", description = "Página de assinaturas")
    @GetMapping("/subscriptions")
    public ResponseEntity<Page<SubscriptionResponse>> subscriptions(SubscriptionFilter filter, Pageable pageable) {
        filter.setCustomerId(authenticatedCustomerService.requireCustomerId());
        return ResponseEntity.ok(subscriptionService.search(filter, pageable).map(SubscriptionResponse::new));
    }

    @Operation(summary = "Uma assinatura do cliente")
    @ApiResponse(responseCode = "200", description = "Assinatura")
    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<SubscriptionResponse> subscription(@PathVariable Long id) {
        return ResponseEntity.ok(new SubscriptionResponse(subscriptionService.findForCustomer(id, authenticatedCustomerService.requireCustomerId())));
    }

    @Operation(summary = "Cancela no fim do período pago, ou agora (apagando o servidor)")
    @ApiResponse(responseCode = "200", description = "Cancelamento registrado")
    @PostMapping("/subscriptions/{id}/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(@PathVariable Long id, @Valid @RequestBody CancelSubscriptionRequest request) {
        return ResponseEntity.ok(new SubscriptionResponse(
            subscriptionService.customerCancel(authenticatedCustomerService.requireCustomerId(), id, request)));
    }

    @Operation(summary = "Desfaz o cancelamento agendado")
    @ApiResponse(responseCode = "200", description = "A assinatura volta a renovar")
    @PostMapping("/subscriptions/{id}/keep")
    public ResponseEntity<SubscriptionResponse> keep(@PathVariable Long id) {
        return ResponseEntity.ok(new SubscriptionResponse(subscriptionService.keepRenewing(authenticatedCustomerService.requireCustomerId(), id)));
    }

    @Operation(summary = "Faturas do cliente")
    @ApiResponse(responseCode = "200", description = "Página de faturas")
    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceResponse>> invoices(InvoiceFilter filter, Pageable pageable) {
        filter.setCustomerId(authenticatedCustomerService.requireCustomerId());
        return ResponseEntity.ok(invoiceService.search(filter, pageable).map(InvoiceResponse::new));
    }

    @Operation(summary = "Uma fatura do cliente")
    @ApiResponse(responseCode = "200", description = "Fatura")
    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceResponse> invoice(@PathVariable Long id) {
        return ResponseEntity.ok(new InvoiceResponse(invoiceService.findForCustomer(id, authenticatedCustomerService.requireCustomerId())));
    }

    @Operation(summary = "Gera o Pix da fatura (ou devolve o que ainda vale)")
    @ApiResponse(responseCode = "200", description = "Fatura com o Pix, ou com as instruções de pagamento manual")
    @PostMapping("/invoices/{id}/pay")
    public ResponseEntity<InvoiceResponse> pay(@PathVariable Long id) {
        return ResponseEntity.ok(new InvoiceResponse(invoiceService.pay(id, authenticatedCustomerService.requireCustomerId())));
    }

    @Operation(summary = "Confere agora se o Pix foi pago")
    @ApiResponse(responseCode = "200", description = "Fatura atualizada")
    @PostMapping("/invoices/{id}/check")
    public ResponseEntity<InvoiceResponse> check(@PathVariable Long id) {
        return ResponseEntity.ok(new InvoiceResponse(invoiceService.checkPayment(id, authenticatedCustomerService.requireCustomerId())));
    }
}
