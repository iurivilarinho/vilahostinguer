package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Preferências do gateway de acesso remoto")
public record GatewaySettingsRequest(

    @Schema(description = "Receber conexões de fora e repassá-las às rotas", example = "true")
    boolean enabled,

    @Schema(description = "Porta compartilhada dos sites HTTP", example = "80", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    int httpPort,

    @Schema(description = "Porta compartilhada dos sites HTTPS", example = "443", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    int tlsPort,

    @Schema(description = "Abrir as portas no roteador automaticamente (UPnP)", example = "false")
    boolean upnpEnabled
) {
}
