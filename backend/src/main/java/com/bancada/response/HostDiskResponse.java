package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Disco deste PC e quanto dele os discos virtuais já reservaram")
public record HostDiskResponse(

    @Schema(description = "Raiz do disco", example = "E:\\")
    String root,

    @Schema(description = "Rótulo do volume", example = "HD Externo")
    String label,

    @Schema(description = "Sistema de arquivos", example = "NTFS")
    String fileSystem,

    @Schema(description = "Tamanho total em bytes")
    long totalBytes,

    @Schema(description = "Espaço livre agora, em bytes")
    long freeBytes,

    @Schema(description = "Soma dos tamanhos dos discos virtuais guardados aqui")
    long allocatedBytes,

    @Schema(description = "Parte dos discos virtuais que ainda vai ocupar espaço à medida que for escrita")
    long reservedBytes,

    @Schema(description = "Quanto ainda cabe em discos novos (livre − reservado − folga)")
    long availableBytes,

    @Schema(description = "É uma unidade removível")
    boolean removable,

    @Schema(description = "Pode guardar discos virtuais")
    boolean supported,

    @Schema(description = "Por que não pode", example = "FAT32 não guarda arquivos maiores que 4 GB.")
    String unsupportedReason
) {
}
