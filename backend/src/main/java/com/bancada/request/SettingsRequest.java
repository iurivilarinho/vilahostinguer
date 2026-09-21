package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Preferências do painel")
public record SettingsRequest(

    @Schema(description = "Procurar dispositivos automaticamente", example = "true")
    boolean scanEnabled,

    @Schema(description = "Intervalo entre buscas, em segundos", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 2, message = "Mínimo de 2 segundos")
    @Max(value = 600, message = "Máximo de 600 segundos")
    int scanIntervalSeconds,

    @Schema(description = "Endereços extras verificados em toda busca", example = "[\"192.168.0.50\"]")
    @NotNull
    List<String> extraHosts,

    @Schema(description = "Usar a credencial padrão e ler as informações de dispositivos novos sozinho", example = "true")
    boolean autoSetup,

    @Schema(description = "Pasta onde os backups são salvos", example = "C:\\\\Users\\\\voce\\\\.bancada\\\\backups",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a pasta dos backups")
    String backupDirectory
) {
}
