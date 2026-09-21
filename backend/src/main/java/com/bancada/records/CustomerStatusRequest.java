package com.bancada.records;

import com.bancada.enums.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Bloqueia, desbloqueia ou encerra a conta de um cliente")
public record CustomerStatusRequest(

    @Schema(description = "Nova situação", example = "SUSPENDED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a situação")
    CustomerStatus status,

    @Schema(description = "Motivo", example = "Uso abusivo da rede")
    @Size(max = 500, message = "Até 500 caracteres")
    String reason
) {
}
