package com.bancada.models;

import com.bancada.enums.RouteStatus;
import com.bancada.enums.RouteType;
import com.bancada.request.RouteRequest;
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

/** Remote access rule: connections reaching this PC are relayed to a service on a device or machine. */
@Entity
@Table(name = "routes")
@Schema(description = "Rota de acesso remoto")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Schema(description = "Tipo")
    private RouteType type;

    @Column(name = "hostname")
    @Schema(description = "Nome acessado (sites)")
    private String hostname;

    @Column(name = "public_port")
    @Schema(description = "Porta aberta no PC (TCP)")
    private Integer publicPort;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Device", nullable = false,
        foreignKey = @ForeignKey(name = "FK_FROM_TBROUTES_FOR_TBDEVICES"))
    @Schema(description = "Dispositivo de destino")
    private Device device;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Machine", foreignKey = @ForeignKey(name = "FK_FROM_TBROUTES_FOR_TBMACHINES"))
    @Schema(description = "Máquina de destino")
    private Machine machine;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Domain", foreignKey = @ForeignKey(name = "FK_FROM_TBROUTES_FOR_TBDOMAINS"))
    @Schema(description = "Domínio cadastrado que cobre o nome")
    private Domain domain;

    @Column(name = "target_port", nullable = false)
    @Schema(description = "Porta do serviço no destino")
    private int targetPort;

    @Column(name = "description")
    @Schema(description = "Observação")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação")
    private RouteStatus status = RouteStatus.ACTIVE;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public Route() {
    }

    public Route(RouteRequest request, String hostname, Device device, Machine machine, Domain domain) {
        update(request, hostname, device, machine, domain);
    }

    public final void update(RouteRequest request, String hostname, Device device, Machine machine, Domain domain) {
        this.type = request.type();
        this.hostname = request.type().isNameBased() ? hostname : null;
        this.publicPort = request.type().isNameBased() ? null : request.publicPort();
        this.device = device;
        this.machine = machine;
        this.domain = request.type().isNameBased() ? domain : null;
        this.targetPort = request.targetPort();
        this.description = request.description() == null || request.description().isBlank() ? null : request.description().trim();
    }

    /** Returns whether the link changed. */
    public boolean linkDomain(Domain domain) {
        Domain linked = type.isNameBased() ? domain : null;
        if (Objects.equals(this.domain, linked)) {
            return false;
        }
        this.domain = linked;
        return true;
    }

    public void changeStatus(RouteStatus target) {
        RouteStatus.validateTransition(this.status, target);
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

    public RouteType getType() {
        return type;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPublicPort() {
        return publicPort;
    }

    public Device getDevice() {
        return device;
    }

    public Machine getMachine() {
        return machine;
    }

    public Domain getDomain() {
        return domain;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public String getDescription() {
        return description;
    }

    public RouteStatus getStatus() {
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
        if (!(other instanceof Route route)) {
            return false;
        }
        return id != null && Objects.equals(id, route.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
