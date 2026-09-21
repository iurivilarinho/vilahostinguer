package com.bancada.controller;

import com.bancada.filter.InvoiceFilter;
import com.bancada.records.InvoiceStatusRequest;
import com.bancada.response.InvoiceResponse;
import com.bancada.service.InvoiceService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@Validated
@Tag(name = "Faturas", description = "Faturas das assinaturas")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Operation(summary = "Lista faturas")
    @ApiResponse(responseCode = "200", description = "Página de faturas")
    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> list(InvoiceFilter filter, Pageable pageable) {
        return ResponseEntity.ok(invoiceService.search(filter, pageable).map(InvoiceResponse::new));
    }

    @Operation(summary = "Busca uma fatura")
    @ApiResponse(responseCode = "200", description = "Fatura")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new InvoiceResponse(invoiceService.findById(id)));
    }

    @Operation(summary = "Confirma o pagamento à mão, ou cancela a fatura")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody InvoiceStatusRequest request) {
        return ResponseEntity.ok(new InvoiceResponse(invoiceService.changeStatus(id, request.status(), request.reason())));
    }
}
