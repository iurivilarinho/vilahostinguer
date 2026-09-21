package com.bancada.models;

import com.bancada.request.PlanRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** A server offer in the storefront: resources, monthly price and the device that hosts it. */
@Entity
@Table(name = "plans")
@Schema(description = "Plano de servidor à venda")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador")
    private Long id;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome")
    private String name;

    @Column(name = "description")
    @Schema(description = "Descrição curta")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Device", nullable = false, foreignKey = @ForeignKey(name = "FK_FROM_TBPLANS_FOR_TBDEVICES"))
    @Schema(description = "Dispositivo onde os servidores são criados")
    private Device device;

    @Column(name = "cpu_limit", nullable = false)
    @Schema(description = "CPUs")
    private Double cpuLimit;

    @Column(name = "memory_mb", nullable = false)
    @Schema(description = "Memória em MB")
    private Integer memoryMb;

    @Column(name = "disk_gb", nullable = false)
    @Schema(description = "Disco anunciado em GB")
    private Integer diskGb;

    @Column(name = "backup_slots", nullable = false, columnDefinition = "integer default 3")
    @Schema(description = "Backups guardados por servidor")
    private int backupSlots;

    @Column(name = "price_monthly", nullable = false, precision = 10, scale = 2)
    @Schema(description = "Preço mensal")
    private BigDecimal priceMonthly;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Aparece na vitrine")
    private boolean active;

    @Column(name = "featured", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Destaque")
    private boolean featured;

    @Column(name = "order_number", nullable = false, columnDefinition = "integer default 0")
    @Schema(description = "Ordem na vitrine")
    private int orderNumber;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Plan() {
    }

    public Plan(PlanRequest request, Device device) {
        update(request, device);
    }

    public final void update(PlanRequest request, Device device) {
        this.name = request.name().trim();
        this.description = request.description() == null || request.description().isBlank() ? null : request.description().trim();
        this.device = device;
        this.cpuLimit = request.cpuLimit();
        this.memoryMb = request.memoryMb();
        this.diskGb = request.diskGb();
        this.backupSlots = request.backupSlots();
        this.priceMonthly = request.priceMonthly();
        this.active = request.active();
        this.featured = request.featured();
        this.orderNumber = request.orderNumber();
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

    public String getDescription() {
        return description;
    }

    public Device getDevice() {
        return device;
    }

    public Double getCpuLimit() {
        return cpuLimit;
    }

    public Integer getMemoryMb() {
        return memoryMb;
    }

    public Integer getDiskGb() {
        return diskGb;
    }

    public int getBackupSlots() {
        return backupSlots;
    }

    public BigDecimal getPriceMonthly() {
        return priceMonthly;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFeatured() {
        return featured;
    }

    public int getOrderNumber() {
        return orderNumber;
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
        if (!(other instanceof Plan plan)) {
            return false;
        }
        return id != null && Objects.equals(id, plan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
