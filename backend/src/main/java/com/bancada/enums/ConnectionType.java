package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Como o dispositivo está ligado a este computador")
public enum ConnectionType {

    @Schema(description = "Cabo USB (rede USB NCM/RNDIS, endereço link-local 169.254.x.x)")
    USB("Cabo USB"),

    @Schema(description = "Rede local ou internet")
    NETWORK("Rede");

    private final String description;

    ConnectionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
