package com.bancada.models;

import com.bancada.enums.CustomerStatus;
import com.bancada.request.CustomerProfileRequest;
import com.bancada.request.CustomerRegisterRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.Locale;
import java.util.Objects;

/** Someone who buys servers through the customer panel. */
@Entity
@Table(name = "customers")
@Schema(description = "Cliente do painel")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador")
    private Long id;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome completo")
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    @Schema(description = "E-mail, também o login")
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    @Schema(description = "Hash BCrypt da senha", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String passwordHash;

    /** Bumped on password change and on "sair de todos os lugares": older tokens stop working. */
    @JsonIgnore
    @Column(name = "token_version", nullable = false, columnDefinition = "integer default 0")
    @Schema(description = "Versão das sessões; muda na troca de senha", accessMode = Schema.AccessMode.WRITE_ONLY)
    private int tokenVersion;

    @Column(name = "phone")
    @Schema(description = "Telefone")
    private String phone;

    @Column(name = "document")
    @Schema(description = "CPF ou CNPJ")
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação da conta")
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "last_login_at")
    @Schema(description = "Último acesso ao painel")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data do cadastro")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    @Schema(description = "Quem cadastrou (cliente pelo próprio painel = o próprio id)")
    private Long createdBy;

    @Column(name = "updated_by")
    @Schema(description = "Quem alterou por último (vazio = administrador ou sistema)")
    private Long updatedBy;

    public Customer() {
    }

    public Customer(CustomerRegisterRequest request, String passwordHash) {
        this.name = request.name().trim();
        this.email = normalizeEmail(request.email());
        this.passwordHash = passwordHash;
        this.phone = blankToNull(request.phone());
        this.document = blankToNull(request.document());
    }

    public void updateProfile(CustomerProfileRequest request) {
        this.name = request.name().trim();
        this.email = normalizeEmail(request.email());
        this.phone = blankToNull(request.phone());
        this.document = blankToNull(request.document());
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.tokenVersion++;
    }

    public void changeStatus(CustomerStatus target) {
        CustomerStatus.validateTransition(this.status, target);
        this.status = target;
        if (target != CustomerStatus.ACTIVE) {
            this.tokenVersion++;
        }
    }

    public void registerLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /** Audit stamping: set by the service, never by entity callbacks. */
    public void stamp(Long actorId) {
        if (this.id == null) {
            this.createdBy = actorId;
        }
        this.updatedBy = actorId;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public String getPhone() {
        return phone;
    }

    public String getDocument() {
        return document;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Customer customer)) {
            return false;
        }
        return id != null && Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
