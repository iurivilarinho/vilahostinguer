package com.bancada.filter;

import com.bancada.enums.DnsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Filtros da lista de domínios")
public class DomainFilter {

    @Schema(description = "Busca pelo nome", example = "duckdns")
    private String search;

    @Schema(description = "Provedores de DNS")
    private List<DnsProvider> provider;

    @Schema(description = "Ativos (verdadeiro) ou arquivados (falso); sem filtro, só os ativos", example = "true")
    private Boolean active;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<DnsProvider> getProvider() {
        return provider;
    }

    public void setProvider(List<DnsProvider> provider) {
        this.provider = provider;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
