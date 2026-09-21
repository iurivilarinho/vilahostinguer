package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sistema de arquivos para formatação")
public enum FileSystemType {

    @Schema(description = "ext4 — padrão para Linux")
    EXT4("ext4", "mkfs.ext4"),

    @Schema(description = "FAT32 — lido por Windows, câmeras e celulares")
    VFAT("vfat", "mkfs.vfat");

    private final String label;
    private final String command;

    FileSystemType(String label, String command) {
        this.label = label;
        this.command = command;
    }

    public String getLabel() {
        return label;
    }

    public String getCommand() {
        return command;
    }
}
