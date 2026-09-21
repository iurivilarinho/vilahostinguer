package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Backup da máquina inteira")
public record MachineBackupRequest(

    @Schema(description = "Nome do backup (vazio = nome da máquina)", example = "Antes de atualizar o PHP")
    @Size(max = 120, message = "Até 120 caracteres")
    String name
) {
}
