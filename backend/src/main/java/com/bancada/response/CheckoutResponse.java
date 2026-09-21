package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da contratação: a assinatura e a primeira fatura a pagar")
public record CheckoutResponse(

    @Schema(description = "Assinatura criada")
    SubscriptionResponse subscription,

    @Schema(description = "Primeira fatura")
    InvoiceResponse invoice
) {
}
