package com.bancada.models;

import com.bancada.enums.ConnectionType;
import com.bancada.enums.DeviceStatus;
import com.bancada.enums.InitSystem;
import com.bancada.enums.PackageManager;
import com.bancada.records.DeviceFacts;
import com.bancada.request.DeviceRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A machine managed by the panel. Its identity is the SSH host key fingerprint, not the address:
 * every phone on the USB cable answers at the same 169.254.1.1, and an IP says nothing about which
 * one is plugged in.
 */
@Entity
@Table(name = "devices")
@Schema(description = "Dispositivo gerenciado pelo painel")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador do dispositivo")
    private Long id;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome dado ao dispositivo")
    private String name;

    @Column(name = "host", nullable = false)
    @Schema(description = "Endereço usado para conectar")
    private String host;

    @Column(name = "port", nullable = false)
    @Schema(description = "Porta do SSH")
    private int port;

    @Column(name = "host_key_fingerprint", nullable = false, unique = true)
    @Schema(description = "Impressão digital SHA-256 da chave do servidor SSH")
    private String hostKeyFingerprint;

    @Column(name = "host_key_type")
    @Schema(description = "Tipo da chave do servidor SSH")
    private String hostKeyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false)
    @Schema(description = "Como o dispositivo está ligado")
    private ConnectionType connectionType;

    @Column(name = "interface_name")
    @Schema(description = "Adaptador de rede deste computador por onde o dispositivo foi visto")
    private String interfaceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação de acesso")
    private DeviceStatus status = DeviceStatus.DISCOVERED;

    @Column(name = "online", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Respondeu na última verificação")
    private boolean online;

    @Column(name = "last_seen_at")
    @Schema(description = "Última vez em que respondeu")
    private LocalDateTime lastSeenAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Credential", foreignKey = @ForeignKey(name = "FK_FROM_TBDEVICES_FOR_TBCREDENTIALS"))
    @Schema(description = "Credencial usada para entrar")
    private Credential credential;

    @Column(name = "hostname")
    @Schema(description = "Nome do host no sistema")
    private String hostname;

    @Column(name = "os_name")
    @Schema(description = "Sistema operacional")
    private String osName;

    @Column(name = "os_version")
    @Schema(description = "Versão do sistema")
    private String osVersion;

    @Column(name = "kernel_version")
    @Schema(description = "Versão do kernel")
    private String kernelVersion;

    @Column(name = "architecture")
    @Schema(description = "Arquitetura do processador")
    private String architecture;

    @Column(name = "cpu_model")
    @Schema(description = "Processador")
    private String cpuModel;

    @Column(name = "cpu_cores")
    @Schema(description = "Núcleos")
    private Integer cpuCores;

    @Column(name = "memory_total_bytes")
    @Schema(description = "Memória total em bytes")
    private Long memoryTotalBytes;

    @Column(name = "disk_total_bytes")
    @Schema(description = "Espaço total da raiz em bytes")
    private Long diskTotalBytes;

    @Column(name = "model")
    @Schema(description = "Modelo do aparelho")
    private String model;

    @Column(name = "mac_address")
    @Schema(description = "Endereço MAC da interface principal")
    private String macAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_manager")
    @Schema(description = "Gerenciador de pacotes")
    private PackageManager packageManager;

    @Enumerated(EnumType.STRING)
    @Column(name = "init_system")
    @Schema(description = "Sistema de inicialização")
    private InitSystem initSystem;

    @Column(name = "home_directory")
    @Schema(description = "Pasta pessoal do usuário da credencial")
    private String homeDirectory;

    @Column(name = "root_access", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "A credencial entra como root")
    private boolean rootAccess;

    @Column(name = "facts_updated_at")
    @Schema(description = "Quando as informações do sistema foram lidas")
    private LocalDateTime factsUpdatedAt;

    @Column(name = "notes", columnDefinition = "text")
    @Schema(description = "Anotações livres")
    private String notes;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Dispositivo ativo; arquivados somem das listas")
    private boolean active = true;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de cadastro")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Device() {
    }

    public Device(String host, int port, String hostKeyFingerprint, String hostKeyType,
                  ConnectionType connectionType, String interfaceName) {
        this.host = host;
        this.port = port;
        this.hostKeyFingerprint = hostKeyFingerprint;
        this.hostKeyType = hostKeyType;
        this.connectionType = connectionType;
        this.interfaceName = interfaceName;
        this.name = "Dispositivo " + host;
        this.online = true;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void update(DeviceRequest request, Credential credential) {
        this.name = request.name().trim();
        this.host = request.host().trim();
        this.port = request.port();
        this.notes = request.notes();
        this.credential = credential;
    }

    public void applyFacts(DeviceFacts facts) {
        this.hostname = facts.hostname();
        this.osName = facts.osName();
        this.osVersion = facts.osVersion();
        this.kernelVersion = facts.kernelVersion();
        this.architecture = facts.architecture();
        this.cpuModel = facts.cpuModel();
        this.cpuCores = facts.cpuCores();
        this.memoryTotalBytes = facts.memoryTotalBytes();
        this.diskTotalBytes = facts.diskTotalBytes();
        this.model = facts.model();
        this.macAddress = facts.macAddress();
        this.packageManager = PackageManager.fromBinary(facts.packageManager());
        this.initSystem = InitSystem.fromLabel(facts.initSystem());
        this.homeDirectory = facts.homeDirectory();
        this.rootAccess = facts.root();
        this.factsUpdatedAt = LocalDateTime.now();
        if (this.name == null || this.name.startsWith("Dispositivo ")) {
            this.name = facts.model() != null && !facts.model().isBlank() ? facts.model() : facts.hostname();
        }
    }

    public void markSeen(String host, ConnectionType connectionType, String interfaceName) {
        this.host = host;
        this.connectionType = connectionType;
        this.interfaceName = interfaceName;
        this.online = true;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void changeStatus(DeviceStatus target) {
        DeviceStatus.validateTransition(this.status, target);
        this.status = target;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getHostKeyFingerprint() {
        return hostKeyFingerprint;
    }

    public String getHostKeyType() {
        return hostKeyType;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public String getHostname() {
        return hostname;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getCpuModel() {
        return cpuModel;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public Long getMemoryTotalBytes() {
        return memoryTotalBytes;
    }

    public Long getDiskTotalBytes() {
        return diskTotalBytes;
    }

    public String getModel() {
        return model;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public InitSystem getInitSystem() {
        return initSystem;
    }

    public String getHomeDirectory() {
        return homeDirectory;
    }

    public boolean isRootAccess() {
        return rootAccess;
    }

    public LocalDateTime getFactsUpdatedAt() {
        return factsUpdatedAt;
    }

    public String getNotes() {
        return notes;
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
        if (!(other instanceof Device device)) {
            return false;
        }
        return id != null && Objects.equals(id, device.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
