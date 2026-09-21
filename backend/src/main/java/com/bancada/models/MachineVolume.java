package com.bancada.models;

import com.bancada.request.MachineVolumeRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
@Schema(description = "Pasta do dispositivo compartilhada com a máquina")
public class MachineVolume {

    @Column(name = "host_path", nullable = false)
    @Schema(description = "Pasta no dispositivo")
    private String hostPath;

    @Column(name = "container_path", nullable = false)
    @Schema(description = "Onde aparece dentro da máquina")
    private String containerPath;

    public MachineVolume() {
    }

    public MachineVolume(String hostPath, String containerPath) {
        this.hostPath = hostPath;
        this.containerPath = containerPath;
    }

    public MachineVolume(MachineVolumeRequest request) {
        this.hostPath = request.hostPath().trim();
        this.containerPath = request.containerPath().trim();
    }

    public String getHostPath() {
        return hostPath;
    }

    public String getContainerPath() {
        return containerPath;
    }
}
