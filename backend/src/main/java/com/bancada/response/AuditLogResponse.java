package com.bancada.response;

import com.bancada.enums.AuditAction;
import com.bancada.models.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Registro da auditoria")
public record AuditLogResponse(

    @Schema(description = "Identificador", example = "40")
    Long id,

    @Schema(description = "Entidade", example = "Subscription")
    String entityType,

    @Schema(description = "Registro", example = "3")
    Long entityId,

    @Schema(description = "Ação")
    AuditAction action,

    @Schema(description = "Descrição da ação", example = "Pagamento")
    String actionDescription,

    @Schema(description = "Cliente que agiu (vazio = administrador ou sistema)", example = "7")
    Long userId,

    @Schema(description = "Quem agiu", example = "Maria Souza")
    String userName,

    @Schema(description = "Quando")
    LocalDateTime occurredAt,

    @Schema(description = "Antes (JSON)")
    String oldValue,

    @Schema(description = "Depois (JSON)")
    String newValue,

    @Schema(description = "Justificativa")
    String reason,

    @Schema(description = "Origem", example = "POST /api/portal/checkout")
    String source
) {

    public AuditLogResponse(AuditLog log) {
        this(log.getId(), log.getEntityType(), log.getEntityId(), log.getAction(), log.getAction().getDescription(), log.getUserId(),
            log.getUserName(), log.getOccurredAt(), log.getOldValue(), log.getNewValue(), log.getReason(), log.getSource());
    }
}
