package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação de uma máquina")
public enum MachineStatus {

    @Schema(description = "Sendo criada (baixando a imagem e preparando o sistema)")
    CREATING("Criando"),

    @Schema(description = "Ligada")
    RUNNING("Ligada"),

    @Schema(description = "Desligada")
    STOPPED("Desligada"),

    @Schema(description = "A criação falhou ou o contêiner sumiu")
    FAILED("Com falha"),

    @Schema(description = "Removida do dispositivo; o registro fica no histórico")
    REMOVED("Removida");

    private static final Map<MachineStatus, Set<MachineStatus>> TRANSITIONS;

    static {
        EnumMap<MachineStatus, Set<MachineStatus>> map = new EnumMap<>(MachineStatus.class);
        map.put(CREATING, Set.of(RUNNING, STOPPED, FAILED));
        map.put(RUNNING, Set.of(STOPPED, FAILED, REMOVED));
        map.put(STOPPED, Set.of(RUNNING, FAILED, REMOVED));
        map.put(FAILED, Set.of(RUNNING, STOPPED, REMOVED));
        map.put(REMOVED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    MachineStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(MachineStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(MachineStatus current, MachineStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação da máquina inválida: " + current + " -> " + target);
        }
    }
}
