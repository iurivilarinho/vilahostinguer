package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Como uma fatura foi paga")
public enum PaymentMethod {

    @Schema(description = "Pix gerado pelo Mercado Pago e confirmado automaticamente")
    PIX_MERCADO_PAGO("Pix (Mercado Pago)"),

    @Schema(description = "Confirmado à mão pelo administrador (Pix direto, transferência, dinheiro)")
    MANUAL("Confirmação manual");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
