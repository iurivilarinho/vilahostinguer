package com.bancada.filter;

import com.bancada.enums.RouteStatus;
import com.bancada.enums.RouteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Filtros da lista de rotas")
public class RouteFilter {

    @Schema(description = "Busca pelo nome acessado ou pela observação", example = "blog")
    private String search;

    @Schema(description = "Tipos de rota")
    private List<RouteType> type;

    @Schema(description = "Situações; sem filtro, as removidas ficam de fora")
    private List<RouteStatus> status;

    @Schema(description = "Dispositivo de destino", example = "1")
    private Long deviceId;

    @Schema(description = "Máquina de destino", example = "3")
    private Long machineId;

    @Schema(description = "Domínio", example = "1")
    private Long domainId;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<RouteType> getType() {
        return type;
    }

    public void setType(List<RouteType> type) {
        this.type = type;
    }

    public List<RouteStatus> getStatus() {
        return status;
    }

    public void setStatus(List<RouteStatus> status) {
        this.status = status;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }
}
