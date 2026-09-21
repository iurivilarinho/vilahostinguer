package com.bancada.response;

import com.bancada.enums.DdnsSyncResult;
import com.bancada.enums.DnsProvider;
import com.bancada.models.Domain;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Domínio com DNS dinâmico (o token nunca sai do painel)")
public record DomainResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome completo", example = "casa.duckdns.org")
    String name,

    @Schema(description = "Provedor de DNS")
    DnsProvider provider,

    @Schema(description = "Nome do provedor", example = "DuckDNS")
    String providerDescription,

    @Schema(description = "Há token ou URL guardado")
    boolean hasSecret,

    @Schema(description = "Atualiza também o curinga")
    boolean wildcard,

    @Schema(description = "Atualização automática ligada")
    boolean ddnsEnabled,

    @Schema(description = "Intervalo máximo entre atualizações, em minutos", example = "30")
    int intervalMinutes,

    @Schema(description = "IP público enviado na última atualização", example = "187.10.20.30")
    String lastIp,

    @Schema(description = "IP para o qual o nome resolvia na última verificação", example = "187.10.20.30")
    String resolvedIp,

    @Schema(description = "Data da última tentativa")
    LocalDateTime lastSyncAt,

    @Schema(description = "Resultado da última tentativa")
    DdnsSyncResult lastResult,

    @Schema(description = "Descrição do resultado", example = "Em dia")
    String lastResultDescription,

    @Schema(description = "Mensagem da última tentativa")
    String lastMessage,

    @Schema(description = "Ativo")
    boolean active,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public DomainResponse(Domain domain) {
        this(domain.getId(), domain.getName(), domain.getProvider(), domain.getProvider().getDescription(),
            domain.getEncryptedSecret() != null, domain.isWildcard(), domain.isDdnsEnabled(), domain.getIntervalMinutes(),
            domain.getLastIp(), domain.getResolvedIp(), domain.getLastSyncAt(), domain.getLastResult(),
            domain.getLastResult().getDescription(), domain.getLastMessage(), domain.isActive(), domain.getCreatedAt(),
            domain.getUpdatedAt());
    }
}
