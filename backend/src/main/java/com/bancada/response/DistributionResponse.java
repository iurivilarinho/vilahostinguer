package com.bancada.response;

import com.bancada.enums.MachineDistribution;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Distribuição disponível para máquinas")
public record DistributionResponse(

    @Schema(description = "Chave", example = "UBUNTU")
    MachineDistribution key,

    @Schema(description = "Nome", example = "Ubuntu")
    String name,

    @Schema(description = "Versões", example = "[\"24.04\", \"22.04\"]")
    List<String> versions,

    @Schema(description = "Tem imagem para a arquitetura do dispositivo")
    boolean supported
) {
}
