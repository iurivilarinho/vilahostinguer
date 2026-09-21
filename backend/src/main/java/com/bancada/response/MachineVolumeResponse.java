package com.bancada.response;

import com.bancada.models.MachineVolume;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pasta compartilhada")
public record MachineVolumeResponse(

    @Schema(description = "Pasta no dispositivo", example = "/srv/sites")
    String hostPath,

    @Schema(description = "Pasta dentro da máquina", example = "/var/www")
    String containerPath
) {

    public MachineVolumeResponse(MachineVolume volume) {
        this(volume.getHostPath(), volume.getContainerPath());
    }
}
