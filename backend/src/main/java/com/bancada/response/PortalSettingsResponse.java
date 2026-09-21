package com.bancada.response;

import com.bancada.enums.CertificateStatus;
import com.bancada.models.PortalSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Preferências do painel do cliente (o token do Mercado Pago nunca sai)")
public record PortalSettingsResponse(

    @Schema(description = "Painel publicado")
    boolean enabled,

    @Schema(description = "Cadastro aberto")
    boolean registrationOpen,

    @Schema(description = "Nome da empresa", example = "VilaHost")
    String companyName,

    @Schema(description = "Endereço do painel", example = "painel.vilahost.com.br")
    String hostname,

    @Schema(description = "Domínio dos sites dos clientes", example = "clientes.vilahost.com.br")
    String customerSitesDomain,

    @Schema(description = "Endereço do SSH mostrado aos clientes", example = "ssh.vilahost.com.br")
    String sshHost,

    @Schema(description = "Primeira porta dos clientes", example = "20000")
    int portRangeStart,

    @Schema(description = "Última porta dos clientes", example = "29999")
    int portRangeEnd,

    @Schema(description = "Dias antes do vencimento para gerar a renovação", example = "7")
    int invoiceDaysBefore,

    @Schema(description = "Dias de atraso até suspender", example = "3")
    int suspendAfterDays,

    @Schema(description = "Dias de atraso até cancelar", example = "15")
    int cancelAfterDays,

    @Schema(description = "Há token do Mercado Pago guardado")
    boolean mercadoPagoConfigured,

    @Schema(description = "Instruções de pagamento manual")
    String manualPaymentInstructions,

    @Schema(description = "E-mail de suporte")
    String supportEmail,

    @Schema(description = "E-mail da conta no Let's Encrypt")
    String acmeEmail,

    @Schema(description = "Certificado HTTPS")
    CertificateStatus certificateStatus,

    @Schema(description = "Descrição da situação do certificado", example = "Ativo")
    String certificateStatusDescription,

    @Schema(description = "Mensagem da última emissão")
    String certificateMessage,

    @Schema(description = "Nome do certificado atual", example = "painel.vilahost.com.br")
    String certificateHostname,

    @Schema(description = "Validade do certificado")
    LocalDateTime certificateExpiresAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public PortalSettingsResponse(PortalSettings settings) {
        this(settings.isEnabled(), settings.isRegistrationOpen(), settings.getCompanyName(), settings.getHostname(),
            settings.getCustomerSitesDomain(), settings.getSshHost(), settings.getPortRangeStart(), settings.getPortRangeEnd(),
            settings.getInvoiceDaysBefore(), settings.getSuspendAfterDays(), settings.getCancelAfterDays(), settings.hasMercadoPago(),
            settings.getManualPaymentInstructions(), settings.getSupportEmail(), settings.getAcmeEmail(), settings.getCertificateStatus(),
            settings.getCertificateStatus().getDescription(), settings.getCertificateMessage(), settings.getCertificateHostname(),
            settings.getCertificateExpiresAt(), settings.getUpdatedAt());
    }
}
