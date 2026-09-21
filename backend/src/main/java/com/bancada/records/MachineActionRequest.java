package com.bancada.records;

import com.bancada.enums.MachineAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ação sobre uma máquina")
public record MachineActionRequest(

    @Schema(description = "Ação", example = "RESTART", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a ação")
    MachineAction action
) {
}
