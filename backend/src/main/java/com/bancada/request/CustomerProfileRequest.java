package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados cadastrais do cliente")
public record CustomerProfileRequest(

    @Schema(description = "Nome completo", example = "Maria Souza", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome")
    @Size(max = 120, message = "Até 120 caracteres")
    String name,

    @Schema(description = "E-mail (também é o login)", example = "maria@exemplo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o e-mail")
    @Email(message = "E-mail inválido")
    @Size(max = 160, message = "Até 160 caracteres")
    String email,

    @Schema(description = "Telefone com DDD", example = "34999990000")
    @Pattern(regexp = "[0-9 ()+-]{0,20}", message = "Telefone inválido")
    String phone,

    @Schema(description = "CPF ou CNPJ", example = "12345678909")
    @Pattern(regexp = "[0-9./-]{0,18}", message = "Documento inválido")
    String document
) {
}
