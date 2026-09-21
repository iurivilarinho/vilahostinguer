package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ação sobre uma máquina")
public enum MachineAction {

    @Schema(description = "Ligar")
    START("Ligar", MachineStatus.RUNNING),

    @Schema(description = "Desligar")
    STOP("Desligar", MachineStatus.STOPPED),

    @Schema(description = "Reiniciar")
    RESTART("Reiniciar", MachineStatus.RUNNING);

    private final String description;
    private final MachineStatus resultingStatus;

    MachineAction(String description, MachineStatus resultingStatus) {
        this.description = description;
        this.resultingStatus = resultingStatus;
    }

    public String getDescription() {
        return description;
    }

    public MachineStatus getResultingStatus() {
        return resultingStatus;
    }
}
