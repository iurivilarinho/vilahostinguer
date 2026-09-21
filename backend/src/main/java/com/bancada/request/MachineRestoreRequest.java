package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Volta a máquina ao estado de um backup dela")
public record MachineRestoreRequest(

    @Schema(description = "Backup da máquina inteira", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha o backup")
    Long backupId
) {
}
