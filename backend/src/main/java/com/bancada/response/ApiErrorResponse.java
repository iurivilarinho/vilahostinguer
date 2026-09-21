package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Resposta padronizada de erro da API")
public record ApiErrorResponse(

    @Schema(description = "Data e hora do erro", example = "2026-09-21T10:15:30", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime timestamp,

    @Schema(description = "Mensagens que descrevem o erro", example = "[\"Dispositivo não encontrado para ID: 5\"]",
        accessMode = Schema.AccessMode.READ_ONLY)
    List<String> message
) {

    public ApiErrorResponse(List<String> message) {
        this(LocalDateTime.now(), message);
    }
}
