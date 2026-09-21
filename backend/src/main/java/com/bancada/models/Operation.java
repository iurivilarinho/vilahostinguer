package com.bancada.models;

import com.bancada.enums.OperationStatus;
import com.bancada.enums.OperationType;
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

/** Something the panel ran on a device on behalf of the user, with its full output. */
@Entity
@Table(name = "operations")
@Schema(description = "Operação executada num dispositivo")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador da operação")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Device", nullable = false,
        foreignKey = @ForeignKey(name = "FK_FROM_TBOPERATIONS_FOR_TBDEVICES"))
    @Schema(description = "Dispositivo onde rodou")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Schema(description = "Tipo da operação")
    private OperationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação")
    private OperationStatus status = OperationStatus.PENDING;

    @Column(name = "title", nullable = false)
    @Schema(description = "Resumo legível do que foi feito")
    private String title;

    @Column(name = "target")
    @Schema(description = "Alvo da operação (aplicativo, partição, backup)")
    private String target;

    @Column(name = "log", columnDefinition = "text")
    @Schema(description = "Saída completa da execução")
    private String log = "";

    @Column(name = "exit_code")
    @Schema(description = "Código de saída do comando")
    private Integer exitCode;

    @Column(name = "started_at")
    @Schema(description = "Início da execução")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    @Schema(description = "Fim da execução")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Operation() {
    }

    public Operation(Device device, OperationType type, String title, String target) {
        this.device = device;
        this.type = type;
        this.title = title;
        this.target = target;
    }

    public void start() {
        OperationStatus.validateTransition(this.status, OperationStatus.RUNNING);
        this.status = OperationStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void finish(OperationStatus target, Integer exitCode) {
        OperationStatus.validateTransition(this.status, target);
        this.status = target;
        this.exitCode = exitCode;
        this.finishedAt = LocalDateTime.now();
    }

    public void appendLog(String chunk) {
        this.log = this.log == null ? chunk : this.log + chunk;
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

    public OperationType getType() {
        return type;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getTarget() {
        return target;
    }

    public String getLog() {
        return log;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
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
        if (!(other instanceof Operation operation)) {
            return false;
        }
        return id != null && Objects.equals(id, operation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
