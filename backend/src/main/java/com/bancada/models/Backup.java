package com.bancada.models;

import com.bancada.enums.BackupKind;
import com.bancada.enums.BackupStatus;
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

/** A tar.gz saved on this computer: chosen folders of a device, or a whole machine (docker export). */
@Entity
@Table(name = "backups")
@Schema(description = "Backup de pastas de um dispositivo")
public class Backup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador do backup")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Device", nullable = false,
        foreignKey = @ForeignKey(name = "FK_FROM_TBBACKUPS_FOR_TBDEVICES"))
    @Schema(description = "Dispositivo de origem")
    private Device device;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Operation",
        foreignKey = @ForeignKey(name = "FK_FROM_TBBACKUPS_FOR_TBOPERATIONS"))
    @Schema(description = "Operação que gerou o backup")
    private Operation operation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Machine", foreignKey = @ForeignKey(name = "FK_FROM_TBBACKUPS_FOR_TBMACHINES"))
    @Schema(description = "Máquina de origem (backups de máquina inteira)")
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, columnDefinition = "varchar(255) default 'FOLDERS'")
    @Schema(description = "O que o backup guarda")
    private BackupKind kind = BackupKind.FOLDERS;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome do backup")
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "backup_paths", joinColumns = @JoinColumn(name = "fk_Id_Backup",
        foreignKey = @ForeignKey(name = "FK_FROM_TBBACKUP_PATHS_FOR_TBBACKUPS")))
    @Column(name = "path", nullable = false)
    @Schema(description = "Pastas incluídas")
    private List<String> paths = new ArrayList<>();

    @Column(name = "file_path")
    @Schema(description = "Arquivo neste computador")
    private String filePath;

    @Column(name = "size_bytes")
    @Schema(description = "Tamanho do arquivo em bytes")
    private Long sizeBytes;

    @Column(name = "sha256")
    @Schema(description = "Hash SHA-256 do arquivo")
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação")
    private BackupStatus status = BackupStatus.CREATING;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Backup() {
    }

    public Backup(Device device, String name, List<String> paths, String filePath) {
        this.device = device;
        this.name = name;
        this.paths = new ArrayList<>(paths);
        this.filePath = filePath;
    }

    /** Whole machine: the container file system, without the shared folders. */
    public Backup(Machine machine, String name, String filePath) {
        this.device = machine.getDevice();
        this.machine = machine;
        this.kind = BackupKind.MACHINE;
        this.name = name;
        this.filePath = filePath;
    }

    public void complete(long sizeBytes, String sha256) {
        changeStatus(BackupStatus.AVAILABLE);
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
    }

    public void changeStatus(BackupStatus target) {
        BackupStatus.validateTransition(this.status, target);
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

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Machine getMachine() {
        return machine;
    }

    public BackupKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public List<String> getPaths() {
        return paths;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public BackupStatus getStatus() {
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
        if (!(other instanceof Backup backup)) {
            return false;
        }
        return id != null && Objects.equals(id, backup.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
