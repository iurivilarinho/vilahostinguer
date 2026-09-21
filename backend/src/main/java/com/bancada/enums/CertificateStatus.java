package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Certificado HTTPS do painel do cliente")
public enum CertificateStatus {

    @Schema(description = "Sem certificado: o painel responde só em HTTP")
    NONE("Sem certificado"),

    @Schema(description = "Pedindo o certificado ao Let's Encrypt")
    ISSUING("Emitindo"),

    @Schema(description = "Certificado válido; o painel responde em HTTPS")
    ACTIVE("Ativo"),

    @Schema(description = "A emissão falhou; a mensagem diz o motivo")
    FAILED("Falhou");

    private final String description;

    CertificateStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
