package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação da conta de um cliente")
public enum CustomerStatus {

    @Schema(description = "Conta ativa: entra no painel e contrata")
    ACTIVE("Ativo"),

    @Schema(description = "Bloqueado pelo administrador: entra só para ver e pagar faturas")
    SUSPENDED("Bloqueado"),

    @Schema(description = "Conta encerrada; o registro fica no histórico")
    CLOSED("Encerrado");

    private static final Map<CustomerStatus, Set<CustomerStatus>> TRANSITIONS;

    static {
        EnumMap<CustomerStatus, Set<CustomerStatus>> map = new EnumMap<>(CustomerStatus.class);
        map.put(ACTIVE, Set.of(SUSPENDED, CLOSED));
        map.put(SUSPENDED, Set.of(ACTIVE, CLOSED));
        map.put(CLOSED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    CustomerStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(CustomerStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(CustomerStatus current, CustomerStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação do cliente inválida: " + current + " -> " + target);
        }
    }
}
