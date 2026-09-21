package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Forma de autenticação da credencial SSH")
public enum CredentialAuthType {

    @Schema(description = "Usuário e senha")
    PASSWORD("Senha"),

    @Schema(description = "Chave privada (OpenSSH ou PEM), com senha opcional")
    PRIVATE_KEY("Chave privada");

    private final String description;

    CredentialAuthType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
