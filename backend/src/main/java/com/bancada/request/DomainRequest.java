package com.bancada.request;

import com.bancada.enums.DnsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Cadastro ou alteração de domínio")
public record DomainRequest(

    @Schema(description = "Nome completo do domínio; no DuckDNS basta o subdomínio", example = "casa.duckdns.org",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o domínio")
    @Size(max = 253, message = "Domínio longo demais")
    @Pattern(regexp = "[A-Za-z0-9.-]+", message = "Use só letras, números, ponto e hífen")
    String name,

    @Schema(description = "Onde o DNS é mantido", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha o provedor")
    DnsProvider provider,

    @Schema(description = "Token da API (Cloudflare, DuckDNS) ou URL de atualização; vazio na alteração mantém o atual")
    @Size(max = 2048, message = "Até 2048 caracteres")
    String secret,

    @Schema(description = "Atualizar também o curinga (*.dominio), para cada site ter seu subdomínio (Cloudflare)", example = "true")
    boolean wildcard,

    @Schema(description = "Manter o DNS apontando para o IP público automaticamente", example = "true")
    boolean ddnsEnabled,

    @Schema(description = "Intervalo máximo entre atualizações, em minutos (mudança de IP é atualizada no minuto seguinte)", example = "30",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "Mínimo de 1 minuto")
    @Max(value = 1440, message = "Máximo de 1440 minutos")
    int intervalMinutes
) {
}
