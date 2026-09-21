package com.bancada.models;

import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineStatus;
import com.bancada.request.MachineRequest;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A Linux virtual machine on this PC (Hyper-V). It lives in a folder on one of the PC disks, has a
 * fixed address on the Bancada network and appears in the panel as a device of its own, peer of
 * the phones and boards, with terminal, apps, files and backups.
 */
@Entity
@Table(name = "machines")
@Schema(description = "Máquina virtual Linux deste PC")
public class Machine {

    public static final String FOLDER = "BancadaVMs";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador")
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Device", foreignKey = @ForeignKey(name = "FK_FROM_TBMACHINES_FOR_TBDEVICES"))
    @Schema(description = "O dispositivo que representa a máquina no painel")
    private Device device;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome")
    private String name;

    @Column(name = "vm_name", nullable = false)
    @Schema(description = "Nome da VM no Hyper-V")
    private String vmName;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution", nullable = false)
    @Schema(description = "Distribuição")
    private MachineDistribution distribution;

    @Column(name = "version", nullable = false)
    @Schema(description = "Versão da distribuição")
    private String version;

    @Column(name = "cpu_count", nullable = false)
    @Schema(description = "Processadores virtuais")
    private int cpuCount;

    @Column(name = "memory_mb", nullable = false)
    @Schema(description = "Memória em MB")
    private int memoryMb;

    @Column(name = "disk_gb", nullable = false)
    @Schema(description = "Tamanho do disco em GB")
    private int diskGb;

    @Column(name = "drive", nullable = false)
    @Schema(description = "Disco do PC onde a máquina fica")
    private String drive;

    @Column(name = "ip_address", nullable = false)
    @Schema(description = "Endereço fixo na rede das máquinas")
    private String ipAddress;

    @Column(name = "mac_address", nullable = false)
    @Schema(description = "Endereço MAC fixo")
    private String macAddress;

    @Column(name = "encrypted_host_key", columnDefinition = "text")
    @Schema(description = "Chave SSH de servidor da máquina, cifrada com a conta do Windows")
    private String encryptedHostKey;

    @Column(name = "username", nullable = false)
    @Schema(description = "Usuário criado na máquina")
    private String username;

    @Column(name = "auto_start", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Liga junto com o PC")
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

    public Machine(MachineRequest request, String drive, String ipAddress, String macAddress) {
        this.name = request.name();
        this.vmName = "bancada-" + request.name();
        this.distribution = request.distribution();
        this.version = request.version();
        this.cpuCount = request.cpuCount();
        this.memoryMb = request.memoryMb();
        this.diskGb = request.diskGb();
        this.drive = drive;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.username = request.username();
        this.autoStart = request.autoStart();
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

    public void attachDevice(Device device, String encryptedHostKey) {
        this.device = device;
        this.encryptedHostKey = encryptedHostKey;
    }

    /** New system for a reinstall; resources, address and user name stay. */
    public void switchSystem(MachineDistribution distribution, String version) {
        this.distribution = distribution;
        this.version = version;
    }

    public void changeStatus(MachineStatus target) {
        MachineStatus.validateTransition(this.status, target);
        this.status = target;
    }

    /** Folder of the machine on the PC disk: configuration, disk and seed. */
    public Path folder() {
        return Paths.get(drive, FOLDER, vmName);
    }

    public Path diskPath() {
        return folder().resolve("disco.vhdx");
    }

    public Path seedPath() {
        return folder().resolve("cloud-init.iso");
    }

    public long diskBytes() {
        return (long) diskGb << 30;
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

    public String getVmName() {
        return vmName;
    }

    public MachineDistribution getDistribution() {
        return distribution;
    }

    public String getVersion() {
        return version;
    }

    public int getCpuCount() {
        return cpuCount;
    }

    public int getMemoryMb() {
        return memoryMb;
    }

    public int getDiskGb() {
        return diskGb;
    }

    public String getDrive() {
        return drive;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getEncryptedHostKey() {
        return encryptedHostKey;
    }

    public String getUsername() {
        return username;
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
