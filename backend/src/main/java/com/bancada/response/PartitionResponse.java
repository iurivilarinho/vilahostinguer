package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Partição ou disco do dispositivo")
public record PartitionResponse(

    @Schema(description = "Nome, sem /dev/", example = "mmcblk0p53")
    String name,

    @Schema(description = "Tamanho em bytes")
    long sizeBytes,

    @Schema(description = "É um disco inteiro (e não uma partição)")
    boolean disk,

    @Schema(description = "Sistema de arquivos detectado", example = "ext4")
    String fileSystem,

    @Schema(description = "Rótulo do sistema de arquivos", example = "dados")
    String label,

    @Schema(description = "Nome da partição na tabela (GPT), como userdata ou boot", example = "userdata")
    String partitionName,

    @Schema(description = "Ponto de montagem, quando montada", example = "/srv")
    String mountPoint,

    @Schema(description = "Espaço usado em bytes, quando montada")
    Long usedBytes,

    @Schema(description = "Pode ser formatada pelo painel")
    boolean formattable,

    @Schema(description = "Por que não pode ser formatada", example = "Partição de sistema do aparelho (modem)")
    String protectionReason
) {
}
