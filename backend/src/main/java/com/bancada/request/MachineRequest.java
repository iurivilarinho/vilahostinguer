package com.bancada.request;

import com.bancada.enums.MachineDistribution;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Nova máquina virtual neste PC")
public record MachineRequest(

    @Schema(description = "Nome (vira o hostname)", example = "web-1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome")
    @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,40}", message = "Use letras minúsculas, números e hífen (2 a 41 caracteres)")
    String name,

    @Schema(description = "Distribuição", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha a distribuição")
    MachineDistribution distribution,

    @Schema(description = "Versão", example = "24.04", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Escolha a versão")
    String version,

    @Schema(description = "Processadores virtuais", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe os processadores")
    @Min(value = 1, message = "Mínimo de 1 processador")
    @Max(value = 64, message = "Máximo de 64 processadores")
    Integer cpuCount,

    @Schema(description = "Memória em MB", example = "2048", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a memória")
    @Min(value = 512, message = "Mínimo de 512 MB")
    @Max(value = 262_144, message = "Máximo de 256 GB")
    Integer memoryMb,

    @Schema(description = "Disco em GB", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe o disco")
    @Min(value = 10, message = "Mínimo de 10 GB")
    @Max(value = 4096, message = "Máximo de 4 TB")
    Integer diskGb,

    @Schema(description = "Disco do PC onde a máquina fica (sem ele, o que tiver mais espaço)", example = "E:\\")
    String drive,

    @Schema(description = "Usuário com sudo", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o usuário")
    @Pattern(regexp = "[a-z_][a-z0-9_-]{0,31}", message = "Usuário Linux inválido")
    String username,

    @Schema(description = "Senha do usuário (SSH e sudo)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a senha")
    @Size(min = 4, max = 128, message = "Use de 4 a 128 caracteres")
    String password,

    @Schema(description = "Liga junto com o PC")
    boolean autoStart
) {
}
