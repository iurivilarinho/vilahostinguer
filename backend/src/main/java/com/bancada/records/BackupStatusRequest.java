package com.bancada.records;

import com.bancada.enums.BackupStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mudança de situação de um backup")
public record BackupStatusRequest(

    @Schema(description = "Nova situação (DISCARDED apaga o arquivo e mantém o registro)", example = "DISCARDED",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a situação")
    BackupStatus status
) {
}
