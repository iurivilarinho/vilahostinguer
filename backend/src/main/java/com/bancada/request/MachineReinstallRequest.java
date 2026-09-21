package com.bancada.request;

import com.bancada.enums.MachineDistribution;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Reinstalação de máquina: sistema do zero, na mesma ou em outra distribuição e versão")
public record MachineReinstallRequest(

    @Schema(description = "Distribuição", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha a distribuição")
    MachineDistribution distribution,

    @Schema(description = "Versão", example = "24.04", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Escolha a versão")
    String version,

    @Schema(description = "Nova senha do usuário (e do sudo)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a senha")
    @Size(min = 4, max = 128, message = "Use de 4 a 128 caracteres")
    String password,

    @Schema(description = "Fazer um backup da máquina inteira antes de apagar", example = "true")
    boolean backupFirst
) {
}
