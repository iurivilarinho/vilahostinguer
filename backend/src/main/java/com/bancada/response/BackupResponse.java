package com.bancada.response;

import com.bancada.enums.BackupStatus;
import com.bancada.models.Backup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Backup de um dispositivo")
public record BackupResponse(

    @Schema(description = "Identificador", example = "3")
    Long id,

    @Schema(description = "Dispositivo de origem")
    DeviceBasicResponse device,

    @Schema(description = "Operação que gerou o backup", example = "12")
    Long operationId,

    @Schema(description = "Nome", example = "Antes de atualizar o nginx")
    String name,

    @Schema(description = "Pastas incluídas", example = "[\"/etc\", \"/root\"]")
    List<String> paths,

    @Schema(description = "Arquivo neste computador")
    String filePath,

    @Schema(description = "Tamanho em bytes")
    Long sizeBytes,

    @Schema(description = "SHA-256 do arquivo")
    String sha256,

    @Schema(description = "Situação")
    BackupStatus status,

    @Schema(description = "Descrição da situação", example = "Disponível")
    String statusDescription,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public BackupResponse(Backup backup) {
        this(backup.getId(), new DeviceBasicResponse(backup.getDevice()),
            backup.getOperation() == null ? null : backup.getOperation().getId(), backup.getName(),
            List.copyOf(backup.getPaths()), backup.getFilePath(), backup.getSizeBytes(), backup.getSha256(),
            backup.getStatus(), backup.getStatus().getDescription(), backup.getCreatedAt(), backup.getUpdatedAt());
    }
}
