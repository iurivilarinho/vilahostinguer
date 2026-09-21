package com.bancada.filter;

import com.bancada.enums.DeviceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Filtros da lista de dispositivos")
public class DeviceFilter {

    @Schema(description = "Busca por nome, endereço, sistema ou modelo", example = "j4")
    private String search;

    @Schema(description = "Situações de acesso")
    private List<DeviceStatus> status;

    @Schema(description = "Somente os que estão respondendo (true) ou fora do ar (false)")
    private Boolean online;

    @Schema(description = "Ativos (padrão true); false lista os arquivados", example = "true")
    private Boolean active = Boolean.TRUE;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<DeviceStatus> getStatus() {
        return status;
    }

    public void setStatus(List<DeviceStatus> status) {
        this.status = status;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
