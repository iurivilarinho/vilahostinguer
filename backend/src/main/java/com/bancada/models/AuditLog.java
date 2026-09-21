package com.bancada.models;

import com.bancada.enums.AuditAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Append-only trail of what people did to customers, subscriptions, invoices and servers. */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "IDX_AUDIT_ENTITY", columnList = "entity_type,entity_id"),
    @Index(name = "IDX_AUDIT_USER", columnList = "user_id"),
    @Index(name = "IDX_AUDIT_AT", columnList = "occurred_at")})
@Schema(description = "Registro imutável de uma movimentação feita por uma pessoa")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador do registro", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(name = "entity_type", length = 80, nullable = false, updatable = false)
    @Schema(description = "Entidade movimentada", example = "Subscription")
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    @Schema(description = "Identificador do registro movimentado", example = "42")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 30, nullable = false, updatable = false)
    @Schema(description = "Ação executada")
    private AuditAction action;

    @Column(name = "user_id", updatable = false)
    @Schema(description = "Cliente que executou a ação (vazio = administrador ou sistema)", example = "7")
    private Long userId;

    @Column(name = "user_name", length = 120, updatable = false)
    @Schema(description = "Nome de quem executou, no momento da ação", example = "Maria Souza")
    private String userName;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    @Schema(description = "Data e hora")
    private LocalDateTime occurredAt;

    @Column(name = "old_value", length = 2000, updatable = false)
    @Schema(description = "Estado anterior em JSON")
    private String oldValue;

    @Column(name = "new_value", length = 2000, updatable = false)
    @Schema(description = "Estado posterior em JSON")
    private String newValue;

    @Column(name = "reason", length = 1000, updatable = false)
    @Schema(description = "Justificativa", example = "Pix recebido no banco")
    private String reason;

    @Column(name = "source", length = 120, updatable = false)
    @Schema(description = "Origem da ação", example = "POST /api/portal/checkout")
    private String source;

    protected AuditLog() {
    }

    public AuditLog(String entityType, Long entityId, AuditAction action, Long userId, String userName, String oldValue,
                    String newValue, String reason, String source) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.userId = userId;
        this.userName = userName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.source = source;
    }

    @PrePersist
    private void prePersist() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getReason() {
        return reason;
    }

    public String getSource() {
        return source;
    }
}
