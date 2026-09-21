package com.bancada.response;

import com.bancada.enums.MachineNetworkMode;
import com.bancada.enums.MachineStatus;
import com.bancada.models.Machine;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de uma máquina")
public record MachineBasicResponse(

    @Schema(description = "Identificador", example = "3")
    Long id,

    @Schema(description = "Nome", example = "web-teste")
    String name,

    @Schema(description = "Rede")
    MachineNetworkMode networkMode,

    @Schema(description = "Situação")
    MachineStatus status
) {

    public MachineBasicResponse(Machine machine) {
        this(machine.getId(), machine.getName(), machine.getNetworkMode(), machine.getStatus());
    }
}
