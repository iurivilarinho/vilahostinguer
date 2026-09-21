package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Situação do Docker num dispositivo")
public record DockerStatusResponse(

    @Schema(description = "Docker instalado")
    boolean installed,

    @Schema(description = "Serviço do Docker respondendo")
    boolean running,

    @Schema(description = "Versão do servidor Docker", example = "28.3.3")
    String version,

    @Schema(description = "Recursos do kernel exigidos pelo Docker que faltam", example = "[\"memory cgroup\", \"overlay\"]")
    List<String> missingKernelFeatures,

    @Schema(description = "Mensagem de erro do Docker, quando não responde")
    String message
) {
}
