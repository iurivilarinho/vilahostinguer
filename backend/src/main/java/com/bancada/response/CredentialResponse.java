package com.bancada.response;

import com.bancada.enums.CredentialAuthType;
import com.bancada.models.Credential;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Credencial SSH (segredos nunca aparecem aqui)")
public record CredentialResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome", example = "Root do J4+")
    String name,

    @Schema(description = "Usuário", example = "root")
    String username,

    @Schema(description = "Forma de autenticação")
    CredentialAuthType authType,

    @Schema(description = "Descrição da forma de autenticação", example = "Senha")
    String authTypeDescription,

    @Schema(description = "Tem senha na chave privada")
    boolean hasPassphrase,

    @Schema(description = "Usada automaticamente em dispositivos novos")
    boolean defaultCredential,

    @Schema(description = "Ativa")
    boolean active,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public CredentialResponse(Credential credential) {
        this(credential.getId(), credential.getName(), credential.getUsername(), credential.getAuthType(),
            credential.getAuthType().getDescription(),
            credential.getEncryptedPassphrase() != null && !credential.getEncryptedPassphrase().isBlank(),
            credential.isDefaultCredential(), credential.isActive(), credential.getCreatedAt(), credential.getUpdatedAt());
    }
}
