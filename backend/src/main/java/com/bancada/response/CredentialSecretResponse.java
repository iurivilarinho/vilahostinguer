package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Segredo de uma credencial, revelado sob pedido explícito")
public record CredentialSecretResponse(

    @Schema(description = "Identificador da credencial", example = "1")
    Long id,

    @Schema(description = "Senha ou chave privada em texto claro")
    String secret,

    @Schema(description = "Senha da chave privada, quando houver")
    String passphrase
) {
}
