package com.bancada.filter;

import com.bancada.enums.BackupStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Filtros da lista de backups")
public class BackupFilter {

    @Schema(description = "Dispositivo", example = "1")
    private Long deviceId;

    @Schema(description = "Busca pelo nome do backup", example = "nginx")
    private String search;

    @Schema(description = "Situações")
    private List<BackupStatus> status;

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

    public List<BackupStatus> getStatus() {
        return status;
    }

    public void setStatus(List<BackupStatus> status) {
        this.status = status;
    }
}
