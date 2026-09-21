package com.bancada.response;

import com.bancada.enums.DeviceEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Evento enviado à interface em tempo real")
public record DeviceEventResponse(

    @Schema(description = "Tipo do evento")
    DeviceEventType type,

    @Schema(description = "Dispositivo envolvido", example = "1")
    Long deviceId,

    @Schema(description = "Nome do dispositivo", example = "Galaxy J4+")
    String deviceName,

    @Schema(description = "Operação envolvida, quando houver", example = "12")
    Long operationId,

    @Schema(description = "Mensagem para mostrar", example = "Novo dispositivo conectado: 169.254.1.1")
    String message,

    @Schema(description = "Momento do evento")
    LocalDateTime occurredAt
) {
}
