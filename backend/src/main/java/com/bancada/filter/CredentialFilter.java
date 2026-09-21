package com.bancada.filter;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filtros da lista de credenciais")
public class CredentialFilter {

    @Schema(description = "Busca por nome ou usuário", example = "root")
    private String search;

    @Schema(description = "Ativas (padrão true); false lista as arquivadas", example = "true")
    private Boolean active = Boolean.TRUE;

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
