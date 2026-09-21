package com.bancada.request;

import com.bancada.enums.BillingCycle;
import com.bancada.enums.MachineDistribution;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Contratação de um servidor")
public record CheckoutRequest(

    @Schema(description = "Plano", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha o plano")
    Long planId,

    @Schema(description = "Período de cobrança", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha o período")
    BillingCycle cycle,

    @Schema(description = "Nome do servidor", example = "meu-site", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Dê um nome ao servidor")
    @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,24}", message = "Use letras minúsculas, números e hífen (2 a 25 caracteres)")
    String hostname,

    @Schema(description = "Sistema operacional", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha o sistema")
    MachineDistribution distribution,

    @Schema(description = "Versão do sistema", example = "24.04", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Escolha a versão")
    String version,

    @Schema(description = "Usuário com sudo criado no servidor", example = "maria", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o usuário")
    @Pattern(regexp = "[a-z_][a-z0-9_-]{0,31}", message = "Usuário Linux inválido (minúsculas, números, _ e -)")
    String username,

    @Schema(description = "Senha do usuário e do sudo (mínimo 8)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a senha")
    @Size(min = 8, max = 128, message = "Use de 8 a 128 caracteres")
    String password
) {
}
