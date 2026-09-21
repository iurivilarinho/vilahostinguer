package com.bancada.records;

import com.bancada.enums.ServiceAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ação sobre o serviço de um aplicativo")
public record ServiceActionRequest(

    @Schema(description = "Ação", example = "RESTART", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a ação")
    ServiceAction action
) {
}
