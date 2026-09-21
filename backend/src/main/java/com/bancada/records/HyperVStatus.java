package com.bancada.records;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Situação do Hyper-V neste PC")
public record HyperVStatus(

    @Schema(description = "O Hyper-V está ativo")
    boolean installed,

    @Schema(description = "O usuário do painel pode controlar o Hyper-V")
    boolean permitted,

    @Schema(description = "A rede das máquinas (switch e NAT) existe")
    boolean network,

    @Schema(description = "Memória total do PC, em MB")
    long memoryMb,

    @Schema(description = "Processadores lógicos do PC")
    int cpus,

    @Schema(description = "O que falta, quando não está pronto")
    String message,

    @Schema(description = "Switch virtual das máquinas", example = "Bancada")
    String switchName,

    @Schema(description = "Rede das máquinas", example = "10.77.0.0/24")
    String network24
) {

    public boolean ready() {
        return installed && permitted && network;
    }
}
