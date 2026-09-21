package com.bancada.records;

import com.bancada.enums.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Suspende, reativa ou cancela uma assinatura pelo administrador")
public record SubscriptionStatusRequest(

    @Schema(description = "Nova situação (SUSPENDED, ACTIVE ou CANCELED)", example = "SUSPENDED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a situação")
    SubscriptionStatus status,

    @Schema(description = "Motivo", example = "Pedido do cliente por telefone")
    @Size(max = 500, message = "Até 500 caracteres")
    String reason
) {
}
