package com.bancada.records;

import com.bancada.enums.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Troca a situação de uma rota (pausar, reativar, remover)")
public record RouteStatusRequest(

    @Schema(description = "Nova situação", example = "PAUSED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a situação")
    RouteStatus status
) {
}
