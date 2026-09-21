package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Máquina recém-criada e a operação que a prepara")
public record MachineCreationResponse(

    @Schema(description = "Máquina (situação Criando até a operação terminar)")
    MachineResponse machine,

    @Schema(description = "Operação de criação, para acompanhar a saída", example = "42")
    Long operationId
) {
}
