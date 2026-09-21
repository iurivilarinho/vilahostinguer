package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação de uma assinatura (um servidor contratado)")
public enum SubscriptionStatus {

    @Schema(description = "Contratada, aguardando o pagamento da primeira fatura")
    PENDING_PAYMENT("Aguardando pagamento"),

    @Schema(description = "Paga; a máquina está sendo criada")
    PROVISIONING("Preparando o servidor"),

    @Schema(description = "Servidor no ar")
    ACTIVE("Ativa"),

    @Schema(description = "Servidor desligado por falta de pagamento; volta ao pagar")
    SUSPENDED("Suspensa"),

    @Schema(description = "Encerrada: servidor removido; o registro fica no histórico")
    CANCELED("Cancelada");

    private static final Map<SubscriptionStatus, Set<SubscriptionStatus>> TRANSITIONS;

    static {
        EnumMap<SubscriptionStatus, Set<SubscriptionStatus>> map = new EnumMap<>(SubscriptionStatus.class);
        map.put(PENDING_PAYMENT, Set.of(PROVISIONING, CANCELED));
        map.put(PROVISIONING, Set.of(ACTIVE, CANCELED));
        map.put(ACTIVE, Set.of(SUSPENDED, CANCELED));
        map.put(SUSPENDED, Set.of(ACTIVE, CANCELED));
        map.put(CANCELED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    SubscriptionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(SubscriptionStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(SubscriptionStatus current, SubscriptionStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação da assinatura inválida: " + current + " -> " + target);
        }
    }
}
