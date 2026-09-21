package com.bancada.request;

import com.bancada.enums.FileSystemType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Pedido de formatação de uma partição")
public record FormatPartitionRequest(

    @Schema(description = "Nome da partição, sem /dev/", example = "mmcblk0p53", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a partição")
    @Pattern(regexp = "[a-zA-Z0-9]+", message = "Nome de partição inválido")
    String partition,

    @Schema(description = "Sistema de arquivos", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha o sistema de arquivos")
    FileSystemType fileSystem,

    @Schema(description = "Rótulo do volume", example = "dados")
    @Pattern(regexp = "[a-zA-Z0-9_-]{0,16}", message = "Use até 16 letras, números, - ou _")
    String label,

    @Schema(description = "Confirmação: o nome da partição digitado de novo", example = "mmcblk0p53",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Digite o nome da partição para confirmar")
    @Size(max = 64)
    String confirmation
) {
}
