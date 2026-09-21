package com.bancada.response;

import com.bancada.enums.MachineStatus;
import com.bancada.models.Machine;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de uma máquina virtual")
public record MachineBasicResponse(

    @Schema(description = "Identificador", example = "3")
    Long id,

    @Schema(description = "Nome", example = "web-1")
    String name,

    @Schema(description = "Endereço na rede das máquinas", example = "10.77.0.10")
    String ipAddress,

    @Schema(description = "Situação")
    MachineStatus status
) {

    public MachineBasicResponse(Machine machine) {
        this(machine.getId(), machine.getName(), machine.getIpAddress(), machine.getStatus());
    }
}
