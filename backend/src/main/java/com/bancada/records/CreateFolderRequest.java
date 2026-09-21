package com.bancada.records;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Criação de pasta no dispositivo")
public record CreateFolderRequest(

    @Schema(description = "Caminho absoluto da nova pasta", example = "/srv/sites", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o caminho")
    String path
) {
}
