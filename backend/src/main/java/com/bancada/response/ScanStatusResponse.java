package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Situação da busca automática de dispositivos")
public record ScanStatusResponse(

    @Schema(description = "Busca automática ligada")
    boolean enabled,

    @Schema(description = "Última busca concluída")
    LocalDateTime lastScanAt,

    @Schema(description = "Adaptadores de rede USB/link-local vistos neste computador", example = "[\"Ethernet 4 (169.254.83.20)\"]")
    List<String> usbInterfaces,

    @Schema(description = "Endereços verificados na última busca", example = "[\"169.254.1.1\"]")
    List<String> probedHosts,

    @Schema(description = "Endereços com SSH respondendo na última busca", example = "[\"169.254.1.1\"]")
    List<String> reachableHosts
) {
}
