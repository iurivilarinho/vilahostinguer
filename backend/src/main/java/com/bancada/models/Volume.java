package com.bancada.models;

import com.bancada.enums.VolumeStatus;
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
 * A virtual disk kept in a file on one of the disks of this PC and lent over the network (NBD) to a
 * device, which sees it as an ordinary disk. It can serve the device itself (mounted at a folder)
 * or one of its machines (the same mount handed into the container).
 */
@Entity
@Table(name = "volumes")
@Schema(description = "Disco virtual guardado num disco deste PC")
public class Volume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador")
    private Long id;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome do disco")
    private String name;

    @Column(name = "drive", nullable = false)
    @Schema(description = "Disco do PC onde o arquivo fica (raiz, como E:\\)")
    private String drive;

    @Column(name = "file_path", nullable = false)
    @Schema(description = "Arquivo do disco neste PC")
    private String filePath;

    @Column(name = "size_bytes", nullable = false)
    @Schema(description = "Tamanho do disco em bytes")
    private long sizeBytes;

    @Column(name = "export_name", nullable = false)
    @Schema(description = "Nome secreto pelo qual o dispositivo pede o disco (NBD)")
    private String exportName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação")
    private VolumeStatus status = VolumeStatus.AVAILABLE;

    @Column(name = "status_message", columnDefinition = "text")
    @Schema(description = "Detalhe da situação (motivo da falha ou da espera)")
    private String statusMessage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Device", foreignKey = @ForeignKey(name = "FK_FROM_TBVOLUMES_FOR_TBDEVICES"))
    @Schema(description = "Dispositivo que recebe o disco")
    private Device device;

    @Column(name = "mount_path")
    @Schema(description = "Pasta do dispositivo onde o disco é montado")
    private String mountPath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Machine", foreignKey = @ForeignKey(name = "FK_FROM_TBVOLUMES_FOR_TBMACHINES"))
    @Schema(description = "Máquina que usa o disco")
    private Machine machine;

    @Column(name = "container_path")
    @Schema(description = "Onde o disco aparece dentro da máquina")
    private String containerPath;

    @Column(name = "formatted", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "O sistema de arquivos já foi criado (nunca é recriado)")
    private boolean formatted;

    @Column(name = "connected_at")
    @Schema(description = "Última vez que o dispositivo conectou")
    private LocalDateTime connectedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Volume() {
    }

    public Volume(String name, String drive, String filePath, long sizeBytes, String exportName) {
        this.name = name;
        this.drive = drive;
        this.filePath = filePath;
        this.sizeBytes = sizeBytes;
        this.exportName = exportName;
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

    public void changeStatus(VolumeStatus target, String message) {
        VolumeStatus.validateTransition(this.status, target);
        this.status = target;
        this.statusMessage = message;
        if (target == VolumeStatus.ATTACHED) {
            this.connectedAt = LocalDateTime.now();
        }
    }

    /** Given to a device (and maybe one of its machines); the attach operation follows. */
    public void assign(Device device, String mountPath, Machine machine, String containerPath) {
        this.device = device;
        this.mountPath = mountPath;
        this.machine = machine;
        this.containerPath = machine == null ? null : containerPath;
    }

    public void release() {
        this.device = null;
        this.mountPath = null;
        this.machine = null;
        this.containerPath = null;
    }

    /** The machine went away; the disk stays mounted on its device. */
    public void forgetMachine() {
        this.machine = null;
        this.containerPath = null;
    }

    public void markFormatted() {
        this.formatted = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDrive() {
        return drive;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getExportName() {
        return exportName;
    }

    public VolumeStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Device getDevice() {
        return device;
    }

    public String getMountPath() {
        return mountPath;
    }

    public Machine getMachine() {
        return machine;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public boolean isFormatted() {
        return formatted;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
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
        if (!(other instanceof Volume volume)) {
            return false;
        }
        return id != null && Objects.equals(id, volume.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
