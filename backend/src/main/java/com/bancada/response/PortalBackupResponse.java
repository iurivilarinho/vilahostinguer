package com.bancada.response;

import com.bancada.enums.BackupStatus;
import com.bancada.models.Backup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Backup de servidor (visão do cliente, sem caminhos do servidor da empresa)")
public record PortalBackupResponse(

    @Schema(description = "Identificador", example = "12")
    Long id,

    @Schema(description = "Nome", example = "Antes de atualizar o PHP")
    String name,

    @Schema(description = "Tamanho em bytes", example = "14578088")
    Long sizeBytes,

    @Schema(description = "Situação")
    BackupStatus status,

    @Schema(description = "Descrição da situação", example = "Disponível")
    String statusDescription,

    @Schema(description = "Tarefa que gerou o backup", example = "30")
    Long operationId,

    @Schema(description = "Data")
    LocalDateTime createdAt
) {

    public PortalBackupResponse(Backup backup) {
        this(backup.getId(), backup.getName(), backup.getSizeBytes(), backup.getStatus(), backup.getStatus().getDescription(),
            backup.getOperation() == null ? null : backup.getOperation().getId(), backup.getCreatedAt());
    }
}
