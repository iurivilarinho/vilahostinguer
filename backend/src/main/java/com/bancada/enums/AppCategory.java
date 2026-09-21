package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categoria de aplicativo do catálogo")
public enum AppCategory {

    @Schema(description = "Servidor web e proxy")
    WEB_SERVER("Servidor web"),

    @Schema(description = "Linguagem e runtime")
    RUNTIME("Linguagens"),

    @Schema(description = "Banco de dados")
    DATABASE("Bancos de dados"),

    @Schema(description = "Contêineres")
    CONTAINER("Contêineres"),

    @Schema(description = "Ferramentas do sistema")
    TOOL("Ferramentas");

    private final String description;

    AppCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
