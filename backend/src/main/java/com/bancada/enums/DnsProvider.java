package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Onde o DNS do domínio é mantido")
public enum DnsProvider {

    @Schema(description = "Cloudflare: o registro A (e o curinga, se pedido) é criado ou atualizado pela API com um token")
    CLOUDFLARE("Cloudflare", true),

    @Schema(description = "DuckDNS: subdomínio gratuito em duckdns.org, atualizado com o token da conta")
    DUCKDNS("DuckDNS", true),

    @Schema(description = "Qualquer serviço de DDNS com URL de atualização; {ip} é trocado pelo IP público")
    CUSTOM_URL("URL de atualização", true),

    @Schema(description = "DNS mantido à mão no provedor; o painel só confere se ele aponta para o IP público")
    MANUAL("Manual", false);

    private final String description;
    private final boolean secretRequired;

    DnsProvider(String description, boolean secretRequired) {
        this.description = description;
        this.secretRequired = secretRequired;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSecretRequired() {
        return secretRequired;
    }
}
