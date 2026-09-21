package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nova senha do usuário do servidor (SSH e sudo)")
public record ServerPasswordRequest(

    @Schema(description = "Nova senha (mínimo 8 caracteres)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a nova senha")
    @Size(min = 8, max = 128, message = "Use de 8 a 128 caracteres")
    String password
) {
}
