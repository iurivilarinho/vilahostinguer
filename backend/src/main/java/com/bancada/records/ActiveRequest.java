package com.bancada.records;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ativa ou arquiva um registro")
public record ActiveRequest(

    @Schema(description = "Ativo", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean active
) {
}
