package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Uso de recursos do dispositivo no momento")
public record DeviceMetricsResponse(

    @Schema(description = "Momento da leitura")
    LocalDateTime collectedAt,

    @Schema(description = "Uso de CPU em %", example = "12.5")
    Double cpuPercent,

    @Schema(description = "Carga média de 1 minuto", example = "0.42")
    Double load1,

    @Schema(description = "Carga média de 5 minutos", example = "0.35")
    Double load5,

    @Schema(description = "Carga média de 15 minutos", example = "0.30")
    Double load15,

    @Schema(description = "Memória total em bytes")
    Long memoryTotalBytes,

    @Schema(description = "Memória em uso em bytes")
    Long memoryUsedBytes,

    @Schema(description = "Swap total em bytes")
    Long swapTotalBytes,

    @Schema(description = "Swap em uso em bytes")
    Long swapUsedBytes,

    @Schema(description = "Espaço total da raiz em bytes")
    Long diskTotalBytes,

    @Schema(description = "Espaço usado da raiz em bytes")
    Long diskUsedBytes,

    @Schema(description = "Tempo ligado em segundos")
    Long uptimeSeconds,

    @Schema(description = "Carga da bateria em %, quando houver", example = "87")
    Integer batteryPercent,

    @Schema(description = "Estado da bateria", example = "Charging")
    String batteryStatus,

    @Schema(description = "Temperatura em °C, quando houver", example = "41.0")
    Double temperatureCelsius,

    @Schema(description = "Processos em execução", example = "96")
    Integer processCount,

    @Schema(description = "Bytes recebidos pelas interfaces de rede desde o boot")
    Long networkReceivedBytes,

    @Schema(description = "Bytes enviados pelas interfaces de rede desde o boot")
    Long networkSentBytes
) {
}
