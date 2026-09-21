package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da última atualização de DNS de um domínio")
public enum DdnsSyncResult {

    @Schema(description = "Ainda não houve atualização")
    PENDING("Aguardando"),

    @Schema(description = "O DNS aponta para o IP público atual")
    SYNCED("Em dia"),

    @Schema(description = "A última tentativa falhou; a mensagem diz o motivo")
    FAILED("Com falha");

    private final String description;

    DdnsSyncResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
