package com.bancada.filter;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filtros da lista de planos")
public class PlanFilter {

    @Schema(description = "Busca pelo nome", example = "vps")
    private String search;

    @Schema(description = "Só os que aparecem na vitrine (verdadeiro) ou só os ocultos (falso)", example = "true")
    private Boolean active;


    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}
