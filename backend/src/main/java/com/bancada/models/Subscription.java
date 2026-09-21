package com.bancada.models;

import com.bancada.enums.BillingCycle;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.request.CheckoutRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** A server a customer bought: plan, billing period and, once paid, the machine that runs it. */
@Entity
@Table(name = "subscriptions")
@Schema(description = "Assinatura de servidor")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Customer", nullable = false, foreignKey = @ForeignKey(name = "FK_FROM_TBSUBSCRIPTIONS_FOR_TBCUSTOMERS"))
    @Schema(description = "Cliente")
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Plan", nullable = false, foreignKey = @ForeignKey(name = "FK_FROM_TBSUBSCRIPTIONS_FOR_TBPLANS"))
    @Schema(description = "Plano")
    private Plan plan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_Id_Machine", foreignKey = @ForeignKey(name = "FK_FROM_TBSUBSCRIPTIONS_FOR_TBMACHINES"))
    @Schema(description = "Máquina criada para a assinatura")
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle", nullable = false)
    @Schema(description = "Período de cobrança")
    private BillingCycle cycle;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    @Schema(description = "Valor de cada período, fixado na contratação")
    private BigDecimal price;

    @Column(name = "hostname", nullable = false)
    @Schema(description = "Nome do servidor escolhido pelo cliente")
    private String hostname;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution", nullable = false)
    @Schema(description = "Sistema operacional escolhido")
    private MachineDistribution distribution;

    @Column(name = "version", nullable = false)
    @Schema(description = "Versão do sistema")
    private String version;

    @Column(name = "username", nullable = false)
    @Schema(description = "Usuário com sudo no servidor")
    private String username;

    /** The chosen password, kept encrypted only until the server is created. */
    @JsonIgnore
    @Column(name = "encrypted_pending_password", columnDefinition = "text")
    @Schema(description = "Senha escolhida, cifrada até o servidor ser criado", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String encryptedPendingPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação")
    private SubscriptionStatus status = SubscriptionStatus.PENDING_PAYMENT;

    @Column(name = "provision_error", columnDefinition = "text")
    @Schema(description = "Por que a criação do servidor falhou")
    private String provisionError;

    @Column(name = "next_due_date")
    @Schema(description = "Fim do período pago (próximo vencimento)")
    private LocalDate nextDueDate;

    @Column(name = "cancel_at_period_end", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Encerra sozinha no fim do período pago")
    private boolean cancelAtPeriodEnd;

    @Column(name = "cancel_reason", columnDefinition = "text")
    @Schema(description = "Motivo do cancelamento")
    private String cancelReason;

    @Column(name = "canceled_at")
    @Schema(description = "Data do cancelamento")
    private LocalDateTime canceledAt;

    @Column(name = "ssh_port")
    @Schema(description = "Porta pública do SSH")
    private Integer sshPort;

    @Column(name = "http_port")
    @Schema(description = "Porta do dispositivo mapeada para a porta 80 do servidor")
    private Integer httpPort;

    @Column(name = "https_port")
    @Schema(description = "Porta do dispositivo mapeada para a porta 443 do servidor")
    private Integer httpsPort;

    @Column(name = "site_hostname")
    @Schema(description = "Endereço do site do servidor")
    private String siteHostname;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data da contratação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    @Schema(description = "Quem contratou (id do cliente)")
    private Long createdBy;

    @Column(name = "updated_by")
    @Schema(description = "Quem alterou por último (vazio = administrador ou sistema)")
    private Long updatedBy;

    public Subscription() {
    }

    public Subscription(CheckoutRequest request, Customer customer, Plan plan, String encryptedPassword) {
        this.customer = customer;
        this.plan = plan;
        this.cycle = request.cycle();
        this.price = request.cycle().priceFor(plan.getPriceMonthly());
        this.hostname = request.hostname();
        this.distribution = request.distribution();
        this.version = request.version();
        this.username = request.username();
        this.encryptedPendingPassword = encryptedPassword;
    }

    public void changeStatus(SubscriptionStatus target) {
        SubscriptionStatus.validateTransition(this.status, target);
        this.status = target;
        if (target == SubscriptionStatus.CANCELED) {
            this.canceledAt = LocalDateTime.now();
        }
    }

    /** Ports and site name reserved for the machine about to be created. */
    public void reserve(Machine machine, int sshPort, int httpPort, int httpsPort, String siteHostname) {
        this.machine = machine;
        this.sshPort = sshPort;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.siteHostname = siteHostname;
        this.provisionError = null;
    }

    /** First activation: the paid period starts today. The pending password is no longer needed. */
    public void activate(LocalDate today) {
        changeStatus(SubscriptionStatus.ACTIVE);
        this.nextDueDate = today.plusMonths(cycle.getMonths());
        this.encryptedPendingPassword = null;
        this.provisionError = null;
    }

    /** A renewal was paid: one more period. */
    public void extend() {
        this.nextDueDate = (nextDueDate == null ? LocalDate.now() : nextDueDate).plusMonths(cycle.getMonths());
    }

    public void failProvisioning(String error) {
        this.provisionError = error;
    }

    public void scheduleCancel(boolean atPeriodEnd, String reason) {
        this.cancelAtPeriodEnd = atPeriodEnd;
        this.cancelReason = reason == null || reason.isBlank() ? null : reason.trim();
    }

    public void keepRenewing() {
        this.cancelAtPeriodEnd = false;
        this.cancelReason = null;
    }

    public void stamp(Long actorId) {
        if (this.id == null) {
            this.createdBy = actorId;
        }
        this.updatedBy = actorId;
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

    public Customer getCustomer() {
        return customer;
    }

    public Plan getPlan() {
        return plan;
    }

    public Machine getMachine() {
        return machine;
    }

    public BillingCycle getCycle() {
        return cycle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getHostname() {
        return hostname;
    }

    public MachineDistribution getDistribution() {
        return distribution;
    }

    public String getVersion() {
        return version;
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPendingPassword() {
        return encryptedPendingPassword;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public String getProvisionError() {
        return provisionError;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public Integer getHttpsPort() {
        return httpsPort;
    }

    public String getSiteHostname() {
        return siteHostname;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Subscription subscription)) {
            return false;
        }
        return id != null && Objects.equals(id, subscription.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
