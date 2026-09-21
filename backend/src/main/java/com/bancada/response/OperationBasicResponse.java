package com.bancada.response;

import com.bancada.enums.OperationStatus;
import com.bancada.enums.OperationType;
import com.bancada.models.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Operação sem a saída completa, para listas")
public record OperationBasicResponse(

    @Schema(description = "Identificador", example = "12")
    Long id,

    @Schema(description = "Dispositivo")
    DeviceBasicResponse device,

    @Schema(description = "Tipo")
    OperationType type,

    @Schema(description = "Descrição do tipo", example = "Instalação de aplicativo")
    String typeDescription,

    @Schema(description = "Situação")
    OperationStatus status,

    @Schema(description = "Descrição da situação", example = "Concluída")
    String statusDescription,

    @Schema(description = "Resumo", example = "Instalar Nginx")
    String title,

    @Schema(description = "Alvo", example = "NGINX")
    String target,

    @Schema(description = "Código de saída", example = "0")
    Integer exitCode,

    @Schema(description = "Início")
    LocalDateTime startedAt,

    @Schema(description = "Fim")
    LocalDateTime finishedAt,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt
) {

    public OperationBasicResponse(Operation operation) {
        this(operation.getId(), new DeviceBasicResponse(operation.getDevice()), operation.getType(),
            operation.getType().getDescription(), operation.getStatus(), operation.getStatus().getDescription(),
            operation.getTitle(), operation.getTarget(), operation.getExitCode(), operation.getStartedAt(),
            operation.getFinishedAt(), operation.getCreatedAt());
    }
}
