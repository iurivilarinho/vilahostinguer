package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Resumo geral para a tela inicial")
public record DashboardSummaryResponse(

    @Schema(description = "Dispositivos cadastrados", example = "3")
    long totalDevices,

    @Schema(description = "Dispositivos respondendo agora", example = "1")
    long onlineDevices,

    @Schema(description = "Dispositivos que pedem atenção (sem credencial ou com acesso recusado)", example = "1")
    long devicesNeedingAttention,

    @Schema(description = "Operações na fila ou em execução", example = "0")
    long runningOperations,

    @Schema(description = "Operações que falharam nas últimas 24 horas", example = "0")
    long failedOperationsLastDay,

    @Schema(description = "Backups disponíveis", example = "4")
    long availableBackups,

    @Schema(description = "Data do backup mais recente")
    LocalDateTime lastBackupAt
) {
}
