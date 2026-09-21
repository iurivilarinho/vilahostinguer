package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uso de recursos de uma máquina ligada")
public record MachineStatsResponse(

    @Schema(description = "Máquina", example = "1")
    Long machineId,

    @Schema(description = "Uso de CPU em % (100% = um núcleo inteiro)", example = "3.2")
    Double cpuPercent,

    @Schema(description = "Memória em uso, como o Docker informa", example = "48.2MiB / 512MiB")
    String memoryUsage,

    @Schema(description = "Memória em uso em % do limite", example = "9.4")
    Double memoryPercent,

    @Schema(description = "Processos dentro da máquina", example = "5")
    Integer processCount
) {
}
