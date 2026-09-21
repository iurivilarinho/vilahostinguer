package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Arquivo ou pasta do dispositivo")
public record FileEntryResponse(

    @Schema(description = "Nome", example = "nginx.conf")
    String name,

    @Schema(description = "Caminho absoluto", example = "/etc/nginx/nginx.conf")
    String path,

    @Schema(description = "É pasta")
    boolean directory,

    @Schema(description = "É link simbólico")
    boolean symlink,

    @Schema(description = "Tamanho em bytes")
    long sizeBytes,

    @Schema(description = "Permissões no formato ls", example = "-rw-r--r--")
    String permissions,

    @Schema(description = "Última modificação")
    LocalDateTime modifiedAt
) {
}
