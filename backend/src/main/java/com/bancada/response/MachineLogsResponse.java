package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Saída recente de uma máquina (docker logs)")
public record MachineLogsResponse(

    @Schema(description = "Máquina", example = "1")
    Long machineId,

    @Schema(description = "Últimas linhas")
    String log
) {
}
