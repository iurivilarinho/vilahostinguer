package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Pedido de backup de pastas de um dispositivo")
public record BackupRequest(

    @Schema(description = "Dispositivo de origem", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe o dispositivo")
    Long deviceId,

    @Schema(description = "Nome do backup", example = "Antes de atualizar o nginx", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome")
    @Size(max = 120)
    String name,

    @Schema(description = "Pastas absolutas a incluir", example = "[\"/etc\", \"/root\", \"/home\"]",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Escolha ao menos uma pasta")
    List<@NotBlank String> paths
) {
}
