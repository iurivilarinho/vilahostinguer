package com.bancada.filter;

import com.bancada.enums.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Filtros da lista de assinaturas")
public class SubscriptionFilter {

    @Schema(description = "Busca pelo nome do servidor ou do cliente", example = "meu-site")
    private String search;

    @Schema(description = "Cliente", example = "7")
    private Long customerId;

    @Schema(description = "Plano", example = "1")
    private Long planId;

    @Schema(description = "Situações")
    private List<SubscriptionStatus> status;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public List<SubscriptionStatus> getStatus() {
        return status;
    }

    public void setStatus(List<SubscriptionStatus> status) {
        this.status = status;
    }
}
