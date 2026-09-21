package com.bancada.response;

import com.bancada.enums.OperationStatus;
import com.bancada.models.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Andamento de uma tarefa num servidor do cliente (sem dados do dispositivo)")
public record PortalOperationResponse(

    @Schema(description = "Identificador", example = "12")
    Long id,

    @Schema(description = "Resumo", example = "Reinstalar meu-site com Debian 12")
    String title,

    @Schema(description = "Situação")
    OperationStatus status,

    @Schema(description = "Descrição da situação", example = "Em execução")
    String statusDescription,

    @Schema(description = "Terminou")
    boolean finished,

    @Schema(description = "Saída")
    String log,

    @Schema(description = "Início")
    LocalDateTime startedAt,

    @Schema(description = "Fim")
    LocalDateTime finishedAt
) {

    public PortalOperationResponse(Operation operation) {
        this(operation.getId(), operation.getTitle(), operation.getStatus(), operation.getStatus().getDescription(),
            operation.getStatus().isFinished(), operation.getLog(), operation.getStartedAt(), operation.getFinishedAt());
    }
}
