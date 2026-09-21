package com.bancada.models;

import com.bancada.enums.DdnsSyncResult;
import com.bancada.enums.DnsProvider;
import com.bancada.request.DomainRequest;
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

/** A domain whose DNS the panel keeps pointed at the public IP of this network (DDNS). */
@Entity
@Table(name = "domains")
@Schema(description = "Domínio com DNS dinâmico")
public class Domain {

    private static final int FAILURE_RETRY_MINUTES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador")
    private Long id;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome completo do domínio")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @Schema(description = "Onde o DNS é mantido")
    private DnsProvider provider;

    @Column(name = "encrypted_secret", columnDefinition = "text")
    @Schema(description = "Token ou URL de atualização, cifrado com a conta do Windows")
    private String encryptedSecret;

    @Column(name = "zone_id")
    @Schema(description = "Zona da Cloudflare encontrada na primeira atualização")
    private String zoneId;

    @Column(name = "wildcard", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Atualiza também o curinga")
    private boolean wildcard;

    @Column(name = "ddns_enabled", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Atualização automática ligada")
    private boolean ddnsEnabled;

    @Column(name = "interval_minutes", nullable = false, columnDefinition = "integer default 30")
    @Schema(description = "Intervalo máximo entre atualizações, em minutos")
    private int intervalMinutes;

    @Column(name = "last_ip")
    @Schema(description = "IP público enviado na última atualização")
    private String lastIp;

    @Column(name = "resolved_ip")
    @Schema(description = "IP para o qual o nome resolvia na última verificação")
    private String resolvedIp;

    @Column(name = "last_sync_at")
    @Schema(description = "Data da última tentativa")
    private LocalDateTime lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_result", nullable = false)
    @Schema(description = "Resultado da última tentativa")
    private DdnsSyncResult lastResult = DdnsSyncResult.PENDING;

    @Column(name = "last_message", columnDefinition = "text")
    @Schema(description = "Mensagem da última tentativa")
    private String lastMessage;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Ativo (falso = arquivado)")
    private boolean active = true;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Domain() {
    }

    public Domain(DomainRequest request, String name, String encryptedSecret) {
        this.name = name;
        this.provider = request.provider();
        this.encryptedSecret = encryptedSecret;
        this.wildcard = request.wildcard();
        this.ddnsEnabled = request.ddnsEnabled();
        this.intervalMinutes = request.intervalMinutes();
    }

    /** A null secret keeps the stored one; a new name, provider or secret forces a new sync. */
    public void update(DomainRequest request, String name, String encryptedSecret) {
        if (!name.equals(this.name) || request.provider() != this.provider) {
            this.zoneId = null;
            this.lastIp = null;
            this.lastResult = DdnsSyncResult.PENDING;
        }
        this.name = name;
        this.provider = request.provider();
        if (encryptedSecret != null) {
            this.encryptedSecret = encryptedSecret;
            this.lastIp = null;
        }
        this.wildcard = request.wildcard();
        this.ddnsEnabled = request.ddnsEnabled();
        this.intervalMinutes = request.intervalMinutes();
    }

    public void recordSync(DdnsSyncResult result, String ip, String resolvedIp, String message, String zoneId) {
        this.lastResult = result;
        this.lastSyncAt = LocalDateTime.now();
        this.lastMessage = message;
        this.resolvedIp = resolvedIp;
        if (result == DdnsSyncResult.SYNCED) {
            this.lastIp = ip;
        }
        if (zoneId != null) {
            this.zoneId = zoneId;
        }
    }

    /** Due when it never ran, the public IP changed, or the interval passed. */
    public boolean isSyncDue(String publicIp, LocalDateTime now) {
        // a failing provider (wrong token) is retried every few minutes, not every minute
        if (lastResult == DdnsSyncResult.FAILED && lastSyncAt != null
            && lastSyncAt.plusMinutes(Math.min(intervalMinutes, FAILURE_RETRY_MINUTES)).isAfter(now)) {
            return false;
        }
        return lastSyncAt == null
            || (publicIp != null && !publicIp.equals(lastIp))
            || lastSyncAt.plusMinutes(intervalMinutes).isBefore(now);
    }

    public boolean covers(String hostname) {
        return hostname.equals(name) || hostname.endsWith("." + name);
    }

    public void changeActive(boolean active) {
        this.active = active;
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

    public DnsProvider getProvider() {
        return provider;
    }

    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public String getZoneId() {
        return zoneId;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public boolean isDdnsEnabled() {
        return ddnsEnabled;
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public String getLastIp() {
        return lastIp;
    }

    public String getResolvedIp() {
        return resolvedIp;
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public DdnsSyncResult getLastResult() {
        return lastResult;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean isActive() {
        return active;
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
        if (!(other instanceof Domain domain)) {
            return false;
        }
        return id != null && Objects.equals(id, domain.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
