package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Cancelamento de assinatura")
public record CancelSubscriptionRequest(

    @Schema(description = "Cancelar só no fim do período já pago (verdadeiro) ou agora, apagando o servidor (falso)", example = "true")
    boolean atPeriodEnd,

    @Schema(description = "Motivo", example = "Não preciso mais do servidor")
    @Size(max = 500, message = "Até 500 caracteres")
    String reason
) {
}
