package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de um dispositivo (cadastro manual ou edição)")
public record DeviceRequest(

    @Schema(description = "Nome do dispositivo", example = "Galaxy J4+ da bancada", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome")
    @Size(max = 120)
    String name,

    @Schema(description = "Endereço (IP ou nome)", example = "169.254.1.1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o endereço")
    @Size(max = 255)
    String host,

    @Schema(description = "Porta do SSH", example = "22", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    int port,

    @Schema(description = "Credencial usada para entrar", example = "1")
    Long credentialId,

    @Schema(description = "Anotações livres")
    @Size(max = 4000)
    String notes
) {
}
