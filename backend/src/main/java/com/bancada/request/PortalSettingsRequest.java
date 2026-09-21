package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Preferências do painel do cliente")
public record PortalSettingsRequest(

    @Schema(description = "Painel publicado (recebe clientes)", example = "true")
    boolean enabled,

    @Schema(description = "Cadastro de clientes novos aberto", example = "true")
    boolean registrationOpen,

    @Schema(description = "Nome da empresa mostrado no painel", example = "VilaHost", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome da empresa")
    @Size(max = 60, message = "Até 60 caracteres")
    String companyName,

    @Schema(description = "Endereço do painel na internet", example = "painel.vilahost.com.br")
    @Size(max = 253, message = "Nome longo demais")
    @Pattern(regexp = "[A-Za-z0-9.-]*", message = "Use só letras, números, ponto e hífen")
    String hostname,

    @Schema(description = "Domínio dos sites dos clientes: cada servidor ganha nome.dominio", example = "clientes.vilahost.com.br")
    @Size(max = 253, message = "Nome longo demais")
    @Pattern(regexp = "[A-Za-z0-9.-]*", message = "Use só letras, números, ponto e hífen")
    String customerSitesDomain,

    @Schema(description = "Endereço mostrado aos clientes para o SSH (vazio = endereço do painel ou IP público)",
        example = "ssh.vilahost.com.br")
    @Size(max = 253, message = "Nome longo demais")
    @Pattern(regexp = "[A-Za-z0-9.-]*", message = "Use só letras, números, ponto e hífen")
    String sshHost,

    @Schema(description = "Primeira porta reservada aos servidores dos clientes", example = "20000")
    @Min(value = 1024, message = "Use portas a partir de 1024")
    @Max(value = 65000, message = "Porta inválida")
    int portRangeStart,

    @Schema(description = "Última porta reservada aos servidores dos clientes", example = "29999")
    @Min(value = 1024, message = "Use portas a partir de 1024")
    @Max(value = 65535, message = "Porta inválida")
    int portRangeEnd,

    @Schema(description = "Dias antes do vencimento para gerar a fatura de renovação", example = "7")
    @Min(value = 1, message = "Mínimo de 1 dia")
    @Max(value = 30, message = "Máximo de 30 dias")
    int invoiceDaysBefore,

    @Schema(description = "Dias de atraso até suspender o servidor", example = "3")
    @Min(value = 0, message = "Mínimo de 0 dias")
    @Max(value = 60, message = "Máximo de 60 dias")
    int suspendAfterDays,

    @Schema(description = "Dias de atraso até cancelar e apagar o servidor", example = "15")
    @Min(value = 1, message = "Mínimo de 1 dia")
    @Max(value = 180, message = "Máximo de 180 dias")
    int cancelAfterDays,

    @Schema(description = "Access token do Mercado Pago (vazio mantém o atual)")
    @Size(max = 200, message = "Até 200 caracteres")
    String mercadoPagoAccessToken,

    @Schema(description = "Apagar o token do Mercado Pago guardado", example = "false")
    boolean removeMercadoPagoToken,

    @Schema(description = "Instruções de pagamento manual (chave Pix, conta)", example = "Pix: financeiro@vilahost.com.br")
    @Size(max = 2000, message = "Até 2000 caracteres")
    String manualPaymentInstructions,

    @Schema(description = "E-mail de suporte mostrado aos clientes", example = "suporte@vilahost.com.br")
    @Email(message = "E-mail inválido")
    @Size(max = 160, message = "Até 160 caracteres")
    String supportEmail,

    @Schema(description = "E-mail da conta no Let's Encrypt (avisos de expiração)", example = "admin@vilahost.com.br")
    @Email(message = "E-mail inválido")
    @Size(max = 160, message = "Até 160 caracteres")
    String acmeEmail
) {
}
