package com.bancada.response;

import com.bancada.enums.RouteType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Porta que o gateway mantém aberta no PC")
public record GatewayListenerResponse(

    @Schema(description = "Porta", example = "80")
    int port,

    @Schema(description = "O que chega por ela")
    RouteType type,

    @Schema(description = "Descrição do tipo", example = "Site (HTTP)")
    String typeDescription,

    @Schema(description = "Aceitando conexões")
    boolean listening,

    @Schema(description = "Motivo de não estar aceitando", example = "A porta 80 já está em uso por outro programa")
    String error,

    @Schema(description = "Rotas atendidas por esta porta", example = "2")
    int routeCount
) {
}
