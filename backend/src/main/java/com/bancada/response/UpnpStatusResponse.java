package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Abertura automática de portas no roteador (UPnP)")
public record UpnpStatusResponse(

    @Schema(description = "Ligada nas preferências")
    boolean enabled,

    @Schema(description = "Roteador com UPnP encontrado")
    boolean found,

    @Schema(description = "Nome do roteador", example = "Archer C6")
    String routerName,

    @Schema(description = "IP deste PC na rede do roteador", example = "192.168.0.10")
    String localAddress,

    @Schema(description = "IP externo informado pelo roteador", example = "187.10.20.30")
    String externalIp,

    @Schema(description = "Portas abertas no roteador")
    List<Integer> mappedPorts,

    @Schema(description = "Último erro", example = "Nenhum roteador respondeu ao UPnP")
    String error,

    @Schema(description = "Última verificação")
    LocalDateTime checkedAt
) {
}
