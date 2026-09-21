package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do envio de arquivo")
public record FileUploadResponse(

    @Schema(description = "Caminho gravado no dispositivo", example = "/srv/site/index.html")
    String path,

    @Schema(description = "Bytes enviados", example = "2048")
    long sizeBytes
) {
}
