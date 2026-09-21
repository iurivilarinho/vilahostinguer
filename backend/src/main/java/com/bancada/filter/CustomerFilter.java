package com.bancada.filter;

import com.bancada.enums.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Filtros da lista de clientes")
public class CustomerFilter {

    @Schema(description = "Busca por nome, e-mail ou documento", example = "maria")
    private String search;

    @Schema(description = "Situações")
    private List<CustomerStatus> status;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<CustomerStatus> getStatus() {
        return status;
    }

    public void setStatus(List<CustomerStatus> status) {
        this.status = status;
    }
}
