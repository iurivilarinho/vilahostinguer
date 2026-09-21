package com.bancada.request;

import com.bancada.enums.RouteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Cadastro ou alteração de rota de acesso remoto")
public record RouteRequest(

    @Schema(description = "Tipo da rota", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha o tipo")
    RouteType type,

    @Schema(description = "Nome acessado (sites HTTP e HTTPS)", example = "blog.casa.duckdns.org")
    @Size(max = 253, message = "Nome longo demais")
    @Pattern(regexp = "[A-Za-z0-9.-]*", message = "Use só letras, números, ponto e hífen")
    String hostname,

    @Schema(description = "Porta aberta no PC (rotas TCP)", example = "2201")
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    Integer publicPort,

    @Schema(description = "Dispositivo de destino (dispensado quando há máquina)", example = "1")
    Long deviceId,

    @Schema(description = "Máquina de destino", example = "3")
    Long machineId,

    @Schema(description = "Porta do serviço no destino (dentro da máquina, quando há máquina)", example = "80",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a porta de destino")
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    Integer targetPort,

    @Schema(description = "Observação", example = "Blog da família")
    @Size(max = 200, message = "Até 200 caracteres")
    String description
) {
}
