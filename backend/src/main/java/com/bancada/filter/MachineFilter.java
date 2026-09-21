package com.bancada.filter;

import com.bancada.enums.MachineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Filtros da lista de máquinas")
public class MachineFilter {

    @Schema(description = "Dispositivo", example = "1")
    private Long deviceId;

    @Schema(description = "Busca pelo nome", example = "web")
    private String search;

    @Schema(description = "Situações; sem filtro, as removidas ficam de fora")
    private List<MachineStatus> status;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<MachineStatus> getStatus() {
        return status;
    }

    public void setStatus(List<MachineStatus> status) {
        this.status = status;
    }
}
