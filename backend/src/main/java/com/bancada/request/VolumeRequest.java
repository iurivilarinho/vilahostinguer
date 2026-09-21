package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Novo disco virtual num disco deste PC")
public record VolumeRequest(

    @Schema(description = "Nome (minúsculas, números e hífen)", example = "dados-site", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,29}$", message = "Use de 2 a 30 letras minúsculas, números ou hífen, começando por letra ou número")
    String name,

    @Schema(description = "Disco do PC (raiz, como a lista de discos devolve)", example = "E:\\", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Escolha o disco do PC")
    String drive,

    @Schema(description = "Tamanho em GB", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe o tamanho")
    @Min(value = 1, message = "O mínimo é 1 GB")
    @Max(value = 16384, message = "O máximo é 16 TB")
    Integer sizeGb
) {
}
