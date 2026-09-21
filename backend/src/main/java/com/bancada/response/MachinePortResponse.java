package com.bancada.response;

import com.bancada.models.MachinePort;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Porta encaminhada")
public record MachinePortResponse(

    @Schema(description = "Porta no dispositivo", example = "2222")
    int hostPort,

    @Schema(description = "Porta dentro da máquina", example = "22")
    int containerPort,

    @Schema(description = "Protocolo", example = "tcp")
    String protocol
) {

    public MachinePortResponse(MachinePort port) {
        this(port.getHostPort(), port.getContainerPort(), port.getProtocol());
    }
}
