package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação de uma rota")
public enum RouteStatus {

    @Schema(description = "Recebendo conexões")
    ACTIVE("Ativa"),

    @Schema(description = "Configurada, mas sem receber conexões")
    PAUSED("Pausada"),

    @Schema(description = "Removida; o registro fica no histórico")
    REMOVED("Removida");

    private static final Map<RouteStatus, Set<RouteStatus>> TRANSITIONS;

    static {
        EnumMap<RouteStatus, Set<RouteStatus>> map = new EnumMap<>(RouteStatus.class);
        map.put(ACTIVE, Set.of(PAUSED, REMOVED));
        map.put(PAUSED, Set.of(ACTIVE, REMOVED));
        map.put(REMOVED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    RouteStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(RouteStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(RouteStatus current, RouteStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação da rota inválida: " + current + " -> " + target);
        }
    }
}
