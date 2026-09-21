package com.bancada.response;

import com.bancada.enums.BillingCycle;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.models.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Assinatura de servidor")
public record SubscriptionResponse(

    @Schema(description = "Identificador", example = "3")
    Long id,

    @Schema(description = "Cliente")
    CustomerBasicResponse customer,

    @Schema(description = "Plano", example = "1")
    Long planId,

    @Schema(description = "Nome do plano", example = "VPS 1")
    String planName,

    @Schema(description = "Máquina (vazio até o pagamento)")
    MachineBasicResponse machine,

    @Schema(description = "Período de cobrança")
    BillingCycle cycle,

    @Schema(description = "Nome do período", example = "Mensal")
    String cycleDescription,

    @Schema(description = "Valor de cada período", example = "19.90")
    BigDecimal price,

    @Schema(description = "Nome do servidor", example = "meu-site")
    String hostname,

    @Schema(description = "Sistema")
    MachineDistribution distribution,

    @Schema(description = "Nome do sistema", example = "Ubuntu")
    String distributionName,

    @Schema(description = "Versão", example = "24.04")
    String version,

    @Schema(description = "Usuário do servidor", example = "maria")
    String username,

    @Schema(description = "Situação")
    SubscriptionStatus status,

    @Schema(description = "Descrição da situação", example = "Ativa")
    String statusDescription,

    @Schema(description = "Por que a criação do servidor falhou")
    String provisionError,

    @Schema(description = "Próximo vencimento", example = "2026-10-21")
    LocalDate nextDueDate,

    @Schema(description = "Encerra no fim do período pago")
    boolean cancelAtPeriodEnd,

    @Schema(description = "Motivo do cancelamento")
    String cancelReason,

    @Schema(description = "Data do cancelamento")
    LocalDateTime canceledAt,

    @Schema(description = "Porta pública do SSH", example = "20000")
    Integer sshPort,

    @Schema(description = "Endereço do site", example = "meu-site.clientes.vilahost.com.br")
    String siteHostname,

    @Schema(description = "Data da contratação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public SubscriptionResponse(Subscription subscription) {
        this(subscription.getId(), new CustomerBasicResponse(subscription.getCustomer()), subscription.getPlan().getId(),
            subscription.getPlan().getName(), subscription.getMachine() == null ? null : new MachineBasicResponse(subscription.getMachine()),
            subscription.getCycle(), subscription.getCycle().getDescription(), subscription.getPrice(), subscription.getHostname(),
            subscription.getDistribution(), subscription.getDistribution().getDisplayName(), subscription.getVersion(),
            subscription.getUsername(), subscription.getStatus(), subscription.getStatus().getDescription(), subscription.getProvisionError(),
            subscription.getNextDueDate(), subscription.isCancelAtPeriodEnd(), subscription.getCancelReason(), subscription.getCanceledAt(),
            subscription.getSshPort(), subscription.getSiteHostname(), subscription.getCreatedAt(), subscription.getUpdatedAt());
    }
}
