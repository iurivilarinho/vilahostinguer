package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "O que um backup guarda")
public enum BackupKind {

    @Schema(description = "Pastas escolhidas de um dispositivo")
    FOLDERS("Pastas do dispositivo"),

    @Schema(description = "A máquina inteira: sistema, programas instalados e arquivos (as pastas compartilhadas ficam de fora)")
    MACHINE("Máquina inteira");

    private final String description;

    BackupKind(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
