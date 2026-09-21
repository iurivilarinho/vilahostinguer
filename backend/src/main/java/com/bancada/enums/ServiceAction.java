package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ação sobre um serviço do sistema")
public enum ServiceAction {

    @Schema(description = "Iniciar")
    START("Iniciar"),

    @Schema(description = "Parar")
    STOP("Parar"),

    @Schema(description = "Reiniciar")
    RESTART("Reiniciar"),

    @Schema(description = "Iniciar junto com o sistema")
    ENABLE("Ativar no boot"),

    @Schema(description = "Não iniciar junto com o sistema")
    DISABLE("Desativar no boot");

    private final String description;

    ServiceAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
