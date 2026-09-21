package com.bancada.models;

import com.bancada.enums.CredentialAuthType;
import com.bancada.request.CredentialRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * SSH login kept in the vault. Secrets are stored already encrypted (DPAPI on Windows) and never
 * leave the backend in listings.
 */
@Entity
@Table(name = "credentials")
@Schema(description = "Credencial SSH guardada no cofre")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador da credencial")
    private Long id;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome para reconhecer a credencial")
    private String name;

    @Column(name = "username", nullable = false)
    @Schema(description = "Usuário do SSH")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false)
    @Schema(description = "Forma de autenticação")
    private CredentialAuthType authType;

    @Column(name = "encrypted_secret", nullable = false, columnDefinition = "text")
    @Schema(description = "Senha ou chave privada cifrada")
    private String encryptedSecret;

    @Column(name = "encrypted_passphrase", columnDefinition = "text")
    @Schema(description = "Senha da chave privada, cifrada")
    private String encryptedPassphrase;

    @Column(name = "default_credential", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Usada automaticamente em dispositivos novos")
    private boolean defaultCredential;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Credencial ativa; inativas somem das listas mas ficam no histórico")
    private boolean active = true;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Credential() {
    }

    public Credential(CredentialRequest request, String encryptedSecret, String encryptedPassphrase) {
        this.name = request.name().trim();
        this.username = request.username().trim();
        this.authType = request.authType();
        this.encryptedSecret = encryptedSecret;
        this.encryptedPassphrase = encryptedPassphrase;
        this.defaultCredential = request.defaultCredential();
    }

    public void update(CredentialRequest request, String newEncryptedSecret, String newEncryptedPassphrase) {
        this.name = request.name().trim();
        this.username = request.username().trim();
        this.authType = request.authType();
        this.defaultCredential = request.defaultCredential();
        if (newEncryptedSecret != null) {
            this.encryptedSecret = newEncryptedSecret;
        }
        if (newEncryptedPassphrase != null) {
            this.encryptedPassphrase = newEncryptedPassphrase;
        }
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public CredentialAuthType getAuthType() {
        return authType;
    }

    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public String getEncryptedPassphrase() {
        return encryptedPassphrase;
    }

    public boolean isDefaultCredential() {
        return defaultCredential;
    }

    public void setDefaultCredential(boolean defaultCredential) {
        this.defaultCredential = defaultCredential;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Credential credential)) {
            return false;
        }
        return id != null && Objects.equals(id, credential.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
