package com.bancada.filter;

import com.bancada.enums.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Filtros da lista de faturas")
public class InvoiceFilter {

    @Schema(description = "Cliente", example = "7")
    private Long customerId;

    @Schema(description = "Assinatura", example = "3")
    private Long subscriptionId;

    @Schema(description = "Situações")
    private List<InvoiceStatus> status;

    @Schema(description = "Só as vencidas e em aberto", example = "false")
    private Boolean overdue;

    @Schema(description = "Vencimento a partir de (inclusivo)", example = "2026-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueFrom;

    @Schema(description = "Vencimento até (inclusivo)", example = "2026-09-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueTo;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public List<InvoiceStatus> getStatus() {
        return status;
    }

    public void setStatus(List<InvoiceStatus> status) {
        this.status = status;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    public LocalDate getDueFrom() {
        return dueFrom;
    }

    public void setDueFrom(LocalDate dueFrom) {
        this.dueFrom = dueFrom;
    }

    public LocalDate getDueTo() {
        return dueTo;
    }

    public void setDueTo(LocalDate dueTo) {
        this.dueTo = dueTo;
    }
}
