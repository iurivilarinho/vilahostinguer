package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Pasta do dispositivo compartilhada com a máquina")
public record MachineVolumeRequest(

    @Schema(description = "Pasta absoluta no dispositivo", example = "/srv/sites", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a pasta do dispositivo")
    @Pattern(regexp = "/[^'\"`$\\\\]*", message = "Use um caminho absoluto sem aspas")
    String hostPath,

    @Schema(description = "Onde a pasta aparece dentro da máquina", example = "/var/www", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a pasta dentro da máquina")
    @Pattern(regexp = "/[^'\"`$\\\\]*", message = "Use um caminho absoluto sem aspas")
    String containerPath
) {
}
