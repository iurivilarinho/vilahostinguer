package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rede da máquina")
public enum MachineNetworkMode {

    @Schema(description = "Rede isolada; só as portas mapeadas ficam acessíveis pelo dispositivo")
    BRIDGE("Isolada com portas"),

    @Schema(description = "Usa a rede do próprio dispositivo (necessário quando a internet dele vem por proxy local)")
    HOST("Rede do dispositivo");

    private final String description;

    MachineNetworkMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
