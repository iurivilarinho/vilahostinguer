package com.bancada.models;

import com.bancada.request.GatewaySettingsRequest;
import com.bancada.request.SettingsRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Single-row table with the panel preferences. */
@Entity
@Table(name = "app_settings")
@Schema(description = "Preferências do painel")
public class AppSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Schema(description = "Identificador fixo (1)")
    private Long id = SINGLETON_ID;

    @Column(name = "scan_enabled", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Procurar dispositivos automaticamente")
    private boolean scanEnabled = true;

    @Column(name = "scan_interval_seconds", nullable = false, columnDefinition = "integer default 5")
    @Schema(description = "Intervalo entre buscas, em segundos")
    private int scanIntervalSeconds = 5;

    @Column(name = "extra_hosts", columnDefinition = "text")
    @Schema(description = "Endereços extras verificados em toda busca, separados por vírgula")
    private String extraHosts = "";

    @Column(name = "auto_setup", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Usar a credencial padrão e ler as informações de dispositivos novos sozinho")
    private boolean autoSetup = true;

    @Column(name = "backup_directory")
    @Schema(description = "Pasta onde os backups são salvos")
    private String backupDirectory;

    @Column(name = "gateway_enabled", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Gateway de acesso remoto ligado")
    private boolean gatewayEnabled = true;

    @Column(name = "gateway_http_port", nullable = false, columnDefinition = "integer default 80")
    @Schema(description = "Porta compartilhada dos sites HTTP")
    private int gatewayHttpPort = 80;

    @Column(name = "gateway_tls_port", nullable = false, columnDefinition = "integer default 443")
    @Schema(description = "Porta compartilhada dos sites HTTPS")
    private int gatewayTlsPort = 443;

    @Column(name = "upnp_enabled", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Abrir as portas do gateway no roteador por UPnP")
    private boolean upnpEnabled;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public AppSettings() {
    }

    public AppSettings(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public void update(SettingsRequest request) {
        this.scanEnabled = request.scanEnabled();
        this.scanIntervalSeconds = request.scanIntervalSeconds();
        this.extraHosts = String.join(",", request.extraHosts());
        this.autoSetup = request.autoSetup();
        this.backupDirectory = request.backupDirectory().trim();
    }

    public void updateGateway(GatewaySettingsRequest request) {
        this.gatewayEnabled = request.enabled();
        this.gatewayHttpPort = request.httpPort();
        this.gatewayTlsPort = request.tlsPort();
        this.upnpEnabled = request.upnpEnabled();
    }

    public List<String> extraHostList() {
        if (extraHosts == null || extraHosts.isBlank()) {
            return List.of();
        }
        return Arrays.stream(extraHosts.split(","))
            .map(String::trim)
            .filter(host -> !host.isEmpty())
            .toList();
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

    public boolean isScanEnabled() {
        return scanEnabled;
    }

    public int getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public boolean isAutoSetup() {
        return autoSetup;
    }

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public boolean isGatewayEnabled() {
        return gatewayEnabled;
    }

    public int getGatewayHttpPort() {
        return gatewayHttpPort;
    }

    public int getGatewayTlsPort() {
        return gatewayTlsPort;
    }

    public boolean isUpnpEnabled() {
        return upnpEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
