package com.bancada.models;

import com.bancada.enums.InvoiceStatus;
import com.bancada.enums.PaymentMethod;
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

/** One billing period of a subscription. */
@Entity
@Table(name = "invoices")
@Schema(description = "Fatura")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador (número da fatura)")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Subscription", nullable = false, foreignKey = @ForeignKey(name = "FK_FROM_TBINVOICES_FOR_TBSUBSCRIPTIONS"))
    @Schema(description = "Assinatura cobrada")
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_Id_Customer", nullable = false, foreignKey = @ForeignKey(name = "FK_FROM_TBINVOICES_FOR_TBCUSTOMERS"))
    @Schema(description = "Cliente")
    private Customer customer;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    @Schema(description = "Valor")
    private BigDecimal amount;

    @Column(name = "description", nullable = false)
    @Schema(description = "Descrição")
    private String description;

    @Column(name = "renewal", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Renovação (falso = primeira fatura da contratação)")
    private boolean renewal;

    @Column(name = "due_date", nullable = false)
    @Schema(description = "Vencimento")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Situação")
    private InvoiceStatus status = InvoiceStatus.OPEN;

    @Column(name = "paid_at")
    @Schema(description = "Data do pagamento")
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    @Schema(description = "Como foi paga")
    private PaymentMethod paymentMethod;

    @Column(name = "provider_payment_id")
    @Schema(description = "Identificador do pagamento no Mercado Pago")
    private String providerPaymentId;

    @Column(name = "pix_code", columnDefinition = "text")
    @Schema(description = "Pix copia e cola")
    private String pixCode;

    @Column(name = "pix_qr_base64", columnDefinition = "text")
    @Schema(description = "QR code do Pix em PNG (base64)")
    private String pixQrBase64;

    @Column(name = "pix_expires_at")
    @Schema(description = "Validade do Pix gerado")
    private LocalDateTime pixExpiresAt;

    @Column(name = "note", columnDefinition = "text")
    @Schema(description = "Observação do administrador")
    private String note;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de emissão")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    @Schema(description = "Quem emitiu (vazio = sistema)")
    private Long createdBy;

    @Column(name = "updated_by")
    @Schema(description = "Quem alterou por último (vazio = administrador ou sistema)")
    private Long updatedBy;

    public Invoice() {
    }

    public Invoice(Subscription subscription, String description, boolean renewal, LocalDate dueDate) {
        this.subscription = subscription;
        this.customer = subscription.getCustomer();
        this.amount = subscription.getPrice();
        this.description = description;
        this.renewal = renewal;
        this.dueDate = dueDate;
    }

    public void attachPix(String providerPaymentId, String pixCode, String pixQrBase64, LocalDateTime expiresAt) {
        this.providerPaymentId = providerPaymentId;
        this.pixCode = pixCode;
        this.pixQrBase64 = pixQrBase64;
        this.pixExpiresAt = expiresAt;
    }

    public void markPaid(PaymentMethod method, String note) {
        InvoiceStatus.validateTransition(this.status, InvoiceStatus.PAID);
        this.status = InvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.paymentMethod = method;
        this.note = note == null || note.isBlank() ? this.note : note.trim();
    }

    public void cancel(String note) {
        InvoiceStatus.validateTransition(this.status, InvoiceStatus.CANCELED);
        this.status = InvoiceStatus.CANCELED;
        this.note = note == null || note.isBlank() ? this.note : note.trim();
    }

    public boolean isOverdue(LocalDate today) {
        return status == InvoiceStatus.OPEN && dueDate.isBefore(today);
    }

    public boolean hasValidPix(LocalDateTime now) {
        return pixCode != null && pixExpiresAt != null && pixExpiresAt.isAfter(now);
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

    public Subscription getSubscription() {
        return subscription;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRenewal() {
        return renewal;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public String getPixCode() {
        return pixCode;
    }

    public String getPixQrBase64() {
        return pixQrBase64;
    }

    public LocalDateTime getPixExpiresAt() {
        return pixExpiresAt;
    }

    public String getNote() {
        return note;
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
        if (!(other instanceof Invoice invoice)) {
            return false;
        }
        return id != null && Objects.equals(id, invoice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
