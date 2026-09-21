package com.bancada.models;

import com.bancada.enums.CertificateStatus;
import com.bancada.request.PortalSettingsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;

/** Single-row table with the customer panel preferences. */
@Entity
@Table(name = "portal_settings")
@Schema(description = "Preferências do painel do cliente")
public class PortalSettings {

    public static final long SINGLETON_ID = 1L;
    public static final int DEFAULT_PORT_RANGE_START = 20_000;
    public static final int DEFAULT_PORT_RANGE_END = 29_999;

    @Id
    @Schema(description = "Identificador fixo (1)")
    private Long id = SINGLETON_ID;

    @Column(name = "enabled", nullable = false, columnDefinition = "boolean default false")
    @Schema(description = "Painel publicado")
    private boolean enabled;

    @Column(name = "registration_open", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "Cadastro de clientes novos aberto")
    private boolean registrationOpen = true;

    @Column(name = "company_name", nullable = false)
    @Schema(description = "Nome da empresa")
    private String companyName = "VilaHost";

    @Column(name = "hostname")
    @Schema(description = "Endereço do painel")
    private String hostname;

    @Column(name = "customer_sites_domain")
    @Schema(description = "Domínio dos sites dos clientes")
    private String customerSitesDomain;

    @Column(name = "ssh_host")
    @Schema(description = "Endereço do SSH mostrado aos clientes")
    private String sshHost;

    @Column(name = "port_range_start", nullable = false, columnDefinition = "integer default 20000")
    @Schema(description = "Primeira porta reservada aos clientes")
    private int portRangeStart = DEFAULT_PORT_RANGE_START;

    @Column(name = "port_range_end", nullable = false, columnDefinition = "integer default 29999")
    @Schema(description = "Última porta reservada aos clientes")
    private int portRangeEnd = DEFAULT_PORT_RANGE_END;

    @Column(name = "invoice_days_before", nullable = false, columnDefinition = "integer default 7")
    @Schema(description = "Dias antes do vencimento para gerar a renovação")
    private int invoiceDaysBefore = 7;

    @Column(name = "suspend_after_days", nullable = false, columnDefinition = "integer default 3")
    @Schema(description = "Dias de atraso até suspender")
    private int suspendAfterDays = 3;

    @Column(name = "cancel_after_days", nullable = false, columnDefinition = "integer default 15")
    @Schema(description = "Dias de atraso até cancelar")
    private int cancelAfterDays = 15;

    @JsonIgnore
    @Column(name = "encrypted_mercado_pago_token", columnDefinition = "text")
    @Schema(description = "Token do Mercado Pago cifrado", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String encryptedMercadoPagoToken;

    @Column(name = "manual_payment_instructions", columnDefinition = "text")
    @Schema(description = "Instruções de pagamento manual")
    private String manualPaymentInstructions;

    @Column(name = "support_email")
    @Schema(description = "E-mail de suporte")
    private String supportEmail;

    @Column(name = "acme_email")
    @Schema(description = "E-mail da conta no Let's Encrypt")
    private String acmeEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_status", nullable = false, columnDefinition = "varchar(255) default 'NONE'")
    @Schema(description = "Situação do certificado HTTPS")
    private CertificateStatus certificateStatus = CertificateStatus.NONE;

    @Column(name = "certificate_message", columnDefinition = "text")
    @Schema(description = "Mensagem da última emissão")
    private String certificateMessage;

    @Column(name = "certificate_hostname")
    @Schema(description = "Nome para o qual o certificado atual foi emitido")
    private String certificateHostname;

    @Column(name = "certificate_expires_at")
    @Schema(description = "Validade do certificado")
    private LocalDateTime certificateExpiresAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última alteração")
    private LocalDateTime updatedAt;

    public PortalSettings() {
    }

    /** A null encrypted token keeps the stored one. */
    public void update(PortalSettingsRequest request, String encryptedMercadoPagoToken) {
        this.enabled = request.enabled();
        this.registrationOpen = request.registrationOpen();
        this.companyName = request.companyName().trim();
        this.hostname = lowerOrNull(request.hostname());
        this.customerSitesDomain = lowerOrNull(request.customerSitesDomain());
        this.sshHost = lowerOrNull(request.sshHost());
        this.portRangeStart = request.portRangeStart();
        this.portRangeEnd = request.portRangeEnd();
        this.invoiceDaysBefore = request.invoiceDaysBefore();
        this.suspendAfterDays = request.suspendAfterDays();
        this.cancelAfterDays = request.cancelAfterDays();
        if (request.removeMercadoPagoToken()) {
            this.encryptedMercadoPagoToken = null;
        } else if (encryptedMercadoPagoToken != null) {
            this.encryptedMercadoPagoToken = encryptedMercadoPagoToken;
        }
        this.manualPaymentInstructions = blankToNull(request.manualPaymentInstructions());
        this.supportEmail = lowerOrNull(request.supportEmail());
        this.acmeEmail = lowerOrNull(request.acmeEmail());
    }

    public void recordCertificate(CertificateStatus status, String message, String hostname, LocalDateTime expiresAt) {
        this.certificateStatus = status;
        this.certificateMessage = message;
        if (status == CertificateStatus.ACTIVE) {
            this.certificateHostname = hostname;
            this.certificateExpiresAt = expiresAt;
        }
    }

    public boolean isPublished() {
        return enabled && hostname != null;
    }

    public boolean hasMercadoPago() {
        return encryptedMercadoPagoToken != null;
    }

    private static String lowerOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRegistrationOpen() {
        return registrationOpen;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getHostname() {
        return hostname;
    }

    public String getCustomerSitesDomain() {
        return customerSitesDomain;
    }

    public String getSshHost() {
        return sshHost;
    }

    public int getPortRangeStart() {
        return portRangeStart;
    }

    public int getPortRangeEnd() {
        return portRangeEnd;
    }

    public int getInvoiceDaysBefore() {
        return invoiceDaysBefore;
    }

    public int getSuspendAfterDays() {
        return suspendAfterDays;
    }

    public int getCancelAfterDays() {
        return cancelAfterDays;
    }

    public String getEncryptedMercadoPagoToken() {
        return encryptedMercadoPagoToken;
    }

    public String getManualPaymentInstructions() {
        return manualPaymentInstructions;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public String getAcmeEmail() {
        return acmeEmail;
    }

    public CertificateStatus getCertificateStatus() {
        return certificateStatus;
    }

    public String getCertificateMessage() {
        return certificateMessage;
    }

    public String getCertificateHostname() {
        return certificateHostname;
    }

    public LocalDateTime getCertificateExpiresAt() {
        return certificateExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
