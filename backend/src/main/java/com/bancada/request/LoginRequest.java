package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Entrada no painel do cliente")
public record LoginRequest(

    @Schema(description = "E-mail", example = "maria@exemplo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o e-mail")
    @Size(max = 160, message = "Até 160 caracteres")
    String email,

    @Schema(description = "Senha", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a senha")
    @Size(max = 128, message = "Até 128 caracteres")
    String password
) {
}
