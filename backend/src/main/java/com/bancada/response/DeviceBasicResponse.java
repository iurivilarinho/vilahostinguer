package com.bancada.response;

import com.bancada.enums.DeviceStatus;
import com.bancada.models.Device;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de um dispositivo")
public record DeviceBasicResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome", example = "Galaxy J4+")
    String name,

    @Schema(description = "Endereço", example = "169.254.1.1")
    String host,

    @Schema(description = "Situação de acesso")
    DeviceStatus status,

    @Schema(description = "Respondeu na última verificação")
    boolean online
) {

    public DeviceBasicResponse(Device device) {
        this(device.getId(), device.getName(), device.getHost(), device.getStatus(), device.isOnline());
    }
}
