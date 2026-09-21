package com.bancada.filter;

import com.bancada.enums.VolumeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Filtros da lista de discos do PC")
public class VolumeFilter {

    @Schema(description = "Busca pelo nome", example = "dados")
    private String search;

    @Schema(description = "Situações; sem filtro, os excluídos ficam de fora")
    private List<VolumeStatus> status;

    @Schema(description = "Dispositivo que recebe o disco", example = "1")
    private Long deviceId;

    @Schema(description = "Máquina que usa o disco", example = "3")
    private Long machineId;

    @Schema(description = "Disco do PC", example = "E:\\")
    private String drive;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<VolumeStatus> getStatus() {
        return status;
    }

    public void setStatus(List<VolumeStatus> status) {
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

    public String getDrive() {
        return drive;
    }

    public void setDrive(String drive) {
        this.drive = drive;
    }
}
