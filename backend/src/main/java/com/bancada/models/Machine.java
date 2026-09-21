package com.bancada.models;

import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineNetworkMode;
import com.bancada.enums.MachineStatus;
import com.bancada.request.MachineRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A Linux machine: a long-running Docker system container on a device. */
@Entity
@Table(name = "machines")
@Schema(description = "Máquina Linux num dispositivo")
public class Machine {

    public static final String CONTAINER_PREFIX = "bancada-";
    public static final int DEFAULT_SSH_PORT = 22;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Device", nullable = false,
        foreignKey = @ForeignKey(name = "FK_FROM_TBMACHINES_FOR_TBDEVICES"))
    @Schema(description = "Dispositivo onde roda")
    private Device device;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome")
    private String name;

    @Column(name = "container_name", nullable = false)
    @Schema(description = "Nome do contêiner no Docker")
    private String containerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution", nullable = false)
    @Schema(description = "Distribuição")
    private MachineDistribution distribution;

    @Column(name = "version", nullable = false)
    @Schema(description = "Versão da distribuição")
    private String version;

    @Column(name = "image", nullable = false)
    @Schema(description = "Imagem Docker")
    private String image;

    @Column(name = "cpu_limit")
    @Schema(description = "Limite de CPUs")
    private Double cpuLimit;

    @Column(name = "memory_limit_mb")
    @Schema(description = "Limite de memória em MB")
    private Integer memoryLimitMb;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_mode", nullable = false)
    @Schema(description = "Rede")
    private MachineNetworkMode networkMode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "machine_ports", joinColumns = @JoinColumn(name = "fk_Id_Machine",
        foreignKey = @ForeignKey(name = "FK_FROM_TBMACHINE_PORTS_FOR_TBMACHINES")))
    @Schema(description = "Portas encaminhadas")
    private List<MachinePort> ports = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "machine_volumes", joinColumns = @JoinColumn(name = "fk_Id_Machine",
        foreignKey = @ForeignKey(name = "FK_FROM_TBMACHINE_VOLUMES_FOR_TBMACHINES")))
    @Schema(description = "Pastas compartilhadas")
    private List<MachineVolume> volumes = new ArrayList<>();

    @Column(name = "username", nullable = false)
    @Schema(description = "Usuário criado na máquina")
    private String username;

    @Column(name = "ssh_enabled", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Servidor SSH instalado")
    private boolean sshEnabled;

    @Column(name = "ssh_port")
    @Schema(description = "Porta do SSH dentro da máquina")
    private Integer sshPort;

    @Column(name = "auto_start", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Liga junto com o dispositivo")
    private boolean autoStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação")
    private MachineStatus status = MachineStatus.CREATING;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Machine() {
    }

    public Machine(MachineRequest request, Device device) {
        this.device = device;
        this.name = request.name();
        this.containerName = CONTAINER_PREFIX + request.name();
        this.distribution = request.distribution();
        this.version = request.version();
        this.image = request.distribution().imageFor(request.version());
        this.cpuLimit = request.cpuLimit();
        this.memoryLimitMb = request.memoryLimitMb();
        this.networkMode = request.networkMode();
        this.ports = request.networkMode() == MachineNetworkMode.HOST ? new ArrayList<>()
            : new ArrayList<>(request.ports().stream().map(MachinePort::new).toList());
        this.volumes = new ArrayList<>(request.volumes().stream().map(MachineVolume::new).toList());
        this.username = request.username();
        this.sshEnabled = request.installSsh();
        this.sshPort = request.installSsh() ? (request.sshPort() == null ? DEFAULT_SSH_PORT : request.sshPort()) : null;
        this.autoStart = request.autoStart();
    }

    public void changeStatus(MachineStatus target) {
        MachineStatus.validateTransition(this.status, target);
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

    public Device getDevice() {
        return device;
    }

    public String getName() {
        return name;
    }

    public String getContainerName() {
        return containerName;
    }

    public MachineDistribution getDistribution() {
        return distribution;
    }

    public String getVersion() {
        return version;
    }

    public String getImage() {
        return image;
    }

    public Double getCpuLimit() {
        return cpuLimit;
    }

    public Integer getMemoryLimitMb() {
        return memoryLimitMb;
    }

    public MachineNetworkMode getNetworkMode() {
        return networkMode;
    }

    public List<MachinePort> getPorts() {
        return ports;
    }

    public List<MachineVolume> getVolumes() {
        return volumes;
    }

    public String getUsername() {
        return username;
    }

    public boolean isSshEnabled() {
        return sshEnabled;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public MachineStatus getStatus() {
        return status;
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
        if (!(other instanceof Machine machine)) {
            return false;
        }
        return id != null && Objects.equals(id, machine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
