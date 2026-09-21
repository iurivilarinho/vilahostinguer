package com.bancada.filter;

import com.bancada.enums.AuditAction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Filtros do histórico de auditoria")
public class AuditLogFilter {

    @Schema(description = "Entidade", example = "Subscription")
    private String entityType;

    @Schema(description = "Identificador do registro", example = "3")
    private Long entityId;

    @Schema(description = "Cliente que agiu", example = "7")
    private Long userId;

    @Schema(description = "Ações")
    private List<AuditAction> action;

    @Schema(description = "A partir de (inclusivo)", example = "2026-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "Até (inclusivo)", example = "2026-09-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<AuditAction> getAction() {
        return action;
    }

    public void setAction(List<AuditAction> action) {
        this.action = action;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
