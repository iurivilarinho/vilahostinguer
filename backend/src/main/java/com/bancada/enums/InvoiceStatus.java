package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação de uma fatura")
public enum InvoiceStatus {

    @Schema(description = "Em aberto, aguardando pagamento")
    OPEN("Em aberto"),

    @Schema(description = "Paga")
    PAID("Paga"),

    @Schema(description = "Cancelada (assinatura encerrada ou fatura substituída)")
    CANCELED("Cancelada");

    private static final Map<InvoiceStatus, Set<InvoiceStatus>> TRANSITIONS;

    static {
        EnumMap<InvoiceStatus, Set<InvoiceStatus>> map = new EnumMap<>(InvoiceStatus.class);
        map.put(OPEN, Set.of(PAID, CANCELED));
        map.put(PAID, Collections.emptySet());
        map.put(CANCELED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    InvoiceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(InvoiceStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(InvoiceStatus current, InvoiceStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação da fatura inválida: " + current + " -> " + target);
        }
    }
}
