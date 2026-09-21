package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Porta encaminhada do dispositivo para a máquina")
public record MachinePortRequest(

    @Schema(description = "Porta no dispositivo", example = "2222", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    int hostPort,

    @Schema(description = "Porta dentro da máquina", example = "22", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    int containerPort,

    @Schema(description = "Protocolo", example = "tcp")
    @Pattern(regexp = "tcp|udp|", message = "Use tcp ou udp")
    String protocol
) {
}
