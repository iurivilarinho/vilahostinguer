package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Este PC como anfitrião das máquinas: Hyper-V, processadores, memória e discos")
public record MachineHostResponse(

    @Schema(description = "O Hyper-V está ativo")
    boolean hyperVInstalled,

    @Schema(description = "O usuário do painel pode controlar o Hyper-V")
    boolean hyperVPermitted,

    @Schema(description = "A rede das máquinas existe")
    boolean networkReady,

    @Schema(description = "Tudo pronto para criar máquinas")
    boolean ready,

    @Schema(description = "O que falta, quando não está pronto")
    String message,

    @Schema(description = "Switch virtual das máquinas", example = "Bancada")
    String switchName,

    @Schema(description = "Rede das máquinas", example = "10.77.0.0/24")
    String network,

    @Schema(description = "Processadores lógicos do PC", example = "12")
    int cpus,

    @Schema(description = "Memória total do PC, em MB", example = "16384")
    long memoryMb,

    @Schema(description = "Memória guardada para o Windows e o painel, em MB", example = "4096")
    long reservedMemoryMb,

    @Schema(description = "Memória já dada às máquinas, em MB", example = "4096")
    long usedMemoryMb,

    @Schema(description = "Memória que ainda cabe em máquinas novas, em MB", example = "8192")
    long availableMemoryMb,

    @Schema(description = "Máquinas existentes", example = "2")
    int machineCount,

    @Schema(description = "Discos do PC")
    List<HostDiskResponse> disks
) {
}
