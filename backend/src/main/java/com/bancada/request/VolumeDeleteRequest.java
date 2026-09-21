package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Confirmação para apagar um disco e tudo o que está nele")
public record VolumeDeleteRequest(

    @Schema(description = "O nome do disco, digitado de novo", example = "dados-site", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Digite o nome do disco para confirmar")
    String confirmation
) {
}
