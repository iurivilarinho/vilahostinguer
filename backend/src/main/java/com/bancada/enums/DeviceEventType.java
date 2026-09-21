package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de evento publicado para a interface")
public enum DeviceEventType {

    @Schema(description = "Um dispositivo novo foi detectado")
    DISCOVERED,

    @Schema(description = "Um dispositivo conhecido voltou a responder")
    ONLINE,

    @Schema(description = "Um dispositivo deixou de responder")
    OFFLINE,

    @Schema(description = "As informações do dispositivo foram atualizadas")
    UPDATED,

    @Schema(description = "Uma operação terminou")
    OPERATION_FINISHED,

    @Schema(description = "Domínios, rotas ou o gateway de acesso remoto mudaram")
    NETWORK_UPDATED
}
