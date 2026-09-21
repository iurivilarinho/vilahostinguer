package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação de uma operação")
public enum OperationStatus {

    @Schema(description = "Na fila — ainda não começou")
    PENDING("Na fila"),

    @Schema(description = "Em execução no dispositivo")
    RUNNING("Em execução"),

    @Schema(description = "Concluída com sucesso")
    SUCCEEDED("Concluída"),

    @Schema(description = "Terminou com erro")
    FAILED("Falhou"),

    @Schema(description = "Cancelada por quem usa o painel")
    CANCELED("Cancelada");

    private static final Map<OperationStatus, Set<OperationStatus>> TRANSITIONS;

    static {
        EnumMap<OperationStatus, Set<OperationStatus>> map = new EnumMap<>(OperationStatus.class);
        map.put(PENDING, Set.of(RUNNING, CANCELED, FAILED));
        map.put(RUNNING, Set.of(SUCCEEDED, FAILED, CANCELED));
        map.put(SUCCEEDED, Collections.emptySet());
        map.put(FAILED, Collections.emptySet());
        map.put(CANCELED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    OperationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinished() {
        return TRANSITIONS.get(this).isEmpty();
    }

    public boolean canTransitionTo(OperationStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(OperationStatus current, OperationStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação da operação inválida: " + current + " -> " + target);
        }
    }
}
