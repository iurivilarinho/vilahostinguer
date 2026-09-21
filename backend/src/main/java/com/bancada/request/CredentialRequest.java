package com.bancada.request;

import com.bancada.enums.CredentialAuthType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de uma credencial SSH")
public record CredentialRequest(

    @Schema(description = "Nome para reconhecer a credencial", example = "Root do J4+", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome")
    @Size(max = 120)
    String name,

    @Schema(description = "Usuário do SSH", example = "root", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o usuário")
    @Size(max = 64)
    String username,

    @Schema(description = "Forma de autenticação", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a forma de autenticação")
    CredentialAuthType authType,

    @Schema(description = "Senha ou conteúdo da chave privada. Na edição, vazio mantém o atual")
    String secret,

    @Schema(description = "Senha da chave privada (opcional). Na edição, vazio mantém a atual")
    String passphrase,

    @Schema(description = "Usar automaticamente em dispositivos novos", example = "true")
    boolean defaultCredential
) {
}
