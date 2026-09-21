package com.bancada.response;

import com.bancada.enums.InvoiceStatus;
import com.bancada.enums.PaymentMethod;
import com.bancada.models.Invoice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Fatura")
public record InvoiceResponse(

    @Schema(description = "Número", example = "15")
    Long id,

    @Schema(description = "Cliente")
    CustomerBasicResponse customer,

    @Schema(description = "Assinatura", example = "3")
    Long subscriptionId,

    @Schema(description = "Nome do servidor", example = "meu-site")
    String hostname,

    @Schema(description = "Valor", example = "19.90")
    BigDecimal amount,

    @Schema(description = "Descrição", example = "VPS 1 — mensal — meu-site")
    String description,

    @Schema(description = "Renovação (falso = primeira fatura)")
    boolean renewal,

    @Schema(description = "Vencimento", example = "2026-10-21")
    LocalDate dueDate,

    @Schema(description = "Vencida e em aberto")
    boolean overdue,

    @Schema(description = "Situação")
    InvoiceStatus status,

    @Schema(description = "Descrição da situação", example = "Em aberto")
    String statusDescription,

    @Schema(description = "Data do pagamento")
    LocalDateTime paidAt,

    @Schema(description = "Como foi paga")
    PaymentMethod paymentMethod,

    @Schema(description = "Descrição da forma de pagamento", example = "Pix (Mercado Pago)")
    String paymentMethodDescription,

    @Schema(description = "Pix copia e cola (quando gerado e ainda válido)")
    String pixCode,

    @Schema(description = "QR code do Pix em PNG (base64)")
    String pixQrBase64,

    @Schema(description = "Validade do Pix")
    LocalDateTime pixExpiresAt,

    @Schema(description = "Observação")
    String note,

    @Schema(description = "Data de emissão")
    LocalDateTime createdAt
) {

    public InvoiceResponse(Invoice invoice) {
        this(invoice.getId(), new CustomerBasicResponse(invoice.getCustomer()), invoice.getSubscription().getId(),
            invoice.getSubscription().getHostname(), invoice.getAmount(), invoice.getDescription(), invoice.isRenewal(), invoice.getDueDate(),
            invoice.isOverdue(LocalDate.now()), invoice.getStatus(), invoice.getStatus().getDescription(), invoice.getPaidAt(),
            invoice.getPaymentMethod(), invoice.getPaymentMethod() == null ? null : invoice.getPaymentMethod().getDescription(),
            invoice.hasValidPix(LocalDateTime.now()) ? invoice.getPixCode() : null,
            invoice.hasValidPix(LocalDateTime.now()) ? invoice.getPixQrBase64() : null, invoice.getPixExpiresAt(), invoice.getNote(),
            invoice.getCreatedAt());
    }
}
