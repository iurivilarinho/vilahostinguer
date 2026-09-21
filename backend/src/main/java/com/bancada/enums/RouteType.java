package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Como uma rota recebe as conexões da internet")
public enum RouteType {

    @Schema(description = "Site: a porta HTTP do gateway é compartilhada e o nome acessado (cabeçalho Host) escolhe o destino")
    HTTP("Site (HTTP)"),

    @Schema(description = "Site seguro: a porta HTTPS do gateway é compartilhada e o nome pedido na conexão (SNI) escolhe o destino; "
        + "o certificado fica no destino")
    TLS("Site seguro (HTTPS)"),

    @Schema(description = "Porta TCP exclusiva repassada inteira ao destino (SSH, banco de dados, jogos...)")
    TCP("Porta TCP");

    private final String description;

    RouteType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isNameBased() {
        return this != TCP;
    }
}
