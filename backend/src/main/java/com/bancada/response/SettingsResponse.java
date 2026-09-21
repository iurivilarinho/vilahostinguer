package com.bancada.response;

import com.bancada.models.AppSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Preferências do painel")
public record SettingsResponse(

    @Schema(description = "Procurar dispositivos automaticamente")
    boolean scanEnabled,

    @Schema(description = "Intervalo entre buscas, em segundos", example = "5")
    int scanIntervalSeconds,

    @Schema(description = "Endereços extras verificados em toda busca")
    List<String> extraHosts,

    @Schema(description = "Usar a credencial padrão e ler as informações de dispositivos novos sozinho")
    boolean autoSetup,

    @Schema(description = "Pasta onde os backups são salvos")
    String backupDirectory,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public SettingsResponse(AppSettings settings) {
        this(settings.isScanEnabled(), settings.getScanIntervalSeconds(), settings.extraHostList(),
            settings.isAutoSetup(), settings.getBackupDirectory(), settings.getUpdatedAt());
    }
}
