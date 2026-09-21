package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de operação executada num dispositivo")
public enum OperationType {

    @Schema(description = "Instalação de aplicativo")
    INSTALL_APP("Instalação de aplicativo"),

    @Schema(description = "Remoção de aplicativo")
    REMOVE_APP("Remoção de aplicativo"),

    @Schema(description = "Ação sobre um serviço (iniciar, parar, reiniciar, ativar no boot)")
    SERVICE_ACTION("Ação de serviço"),

    @Schema(description = "Atualização dos pacotes do sistema")
    SYSTEM_UPGRADE("Atualização do sistema"),

    @Schema(description = "Criação de backup")
    BACKUP("Backup"),

    @Schema(description = "Restauração de backup")
    RESTORE("Restauração de backup"),

    @Schema(description = "Formatação de partição")
    FORMAT_PARTITION("Formatação de partição"),

    @Schema(description = "Criação de máquina")
    MACHINE_CREATE("Criação de máquina"),

    @Schema(description = "Ligar, desligar ou reiniciar máquina")
    MACHINE_ACTION("Ação em máquina"),

    @Schema(description = "Remoção de máquina")
    MACHINE_REMOVE("Remoção de máquina"),

    @Schema(description = "Backup da máquina inteira (sistema, programas e arquivos)")
    MACHINE_BACKUP("Backup de máquina"),

    @Schema(description = "Volta a máquina ao estado de um backup")
    MACHINE_RESTORE("Restauração de máquina"),

    @Schema(description = "Reinstala a máquina do zero, na mesma ou em outra distribuição e versão")
    MACHINE_REINSTALL("Reinstalação de máquina"),

    @Schema(description = "Conexão de um disco do PC ao dispositivo ou a uma máquina")
    VOLUME_ATTACH("Conexão de disco do PC"),

    @Schema(description = "Desconexão de um disco do PC")
    VOLUME_DETACH("Desconexão de disco do PC");

    private final String description;

    OperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
