package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Tráfego de uma rota desde que o painel abriu")
public record RouteTrafficResponse(

    @Schema(description = "Rota", example = "1")
    Long routeId,

    @Schema(description = "Conexões abertas agora", example = "1")
    int activeConnections,

    @Schema(description = "Conexões recebidas", example = "42")
    long totalConnections,

    @Schema(description = "Bytes recebidos de fora", example = "10240")
    long bytesIn,

    @Schema(description = "Bytes enviados para fora", example = "204800")
    long bytesOut,

    @Schema(description = "Última conexão")
    LocalDateTime lastConnectionAt,

    @Schema(description = "Último erro", example = "O destino 169.254.1.1:80 recusou a conexão")
    String lastError,

    @Schema(description = "Quando o último erro aconteceu")
    LocalDateTime lastErrorAt
) {
}
