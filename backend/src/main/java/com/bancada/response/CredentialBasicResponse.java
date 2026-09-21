package com.bancada.response;

import com.bancada.enums.CredentialAuthType;
import com.bancada.models.Credential;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de uma credencial (sem segredos)")
public record CredentialBasicResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome", example = "Root do J4+")
    String name,

    @Schema(description = "Usuário", example = "root")
    String username,

    @Schema(description = "Forma de autenticação")
    CredentialAuthType authType
) {

    public CredentialBasicResponse(Credential credential) {
        this(credential.getId(), credential.getName(), credential.getUsername(), credential.getAuthType());
    }
}
