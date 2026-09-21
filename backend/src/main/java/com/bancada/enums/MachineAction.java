package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ação sobre uma máquina")
public enum MachineAction {

    @Schema(description = "Ligar")
    START("Ligar", "start", MachineStatus.RUNNING),

    @Schema(description = "Desligar")
    STOP("Desligar", "stop", MachineStatus.STOPPED),

    @Schema(description = "Reiniciar")
    RESTART("Reiniciar", "restart", MachineStatus.RUNNING);

    private final String description;
    private final String dockerCommand;
    private final MachineStatus resultingStatus;

    MachineAction(String description, String dockerCommand, MachineStatus resultingStatus) {
        this.description = description;
        this.dockerCommand = dockerCommand;
        this.resultingStatus = resultingStatus;
    }

    public String getDescription() {
        return description;
    }

    public String getDockerCommand() {
        return dockerCommand;
    }

    public MachineStatus getResultingStatus() {
        return resultingStatus;
    }
}
