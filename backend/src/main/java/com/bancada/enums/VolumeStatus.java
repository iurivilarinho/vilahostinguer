package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação de um disco do PC")
public enum VolumeStatus {

    @Schema(description = "Criado neste PC e não entregue a nenhum dispositivo")
    AVAILABLE("Livre"),

    @Schema(description = "Sendo conectado e montado no dispositivo")
    ATTACHING("Conectando"),

    @Schema(description = "O dispositivo está com o disco conectado")
    ATTACHED("Conectado"),

    @Schema(description = "Entregue a um dispositivo que está sem conexão com ele (desligado, reiniciando ou fora do cabo); volta sozinho")
    WAITING("Aguardando o dispositivo"),

    @Schema(description = "Sendo desmontado e desconectado")
    DETACHING("Desconectando"),

    @Schema(description = "A última conexão ou desconexão falhou")
    FAILED("Com falha"),

    @Schema(description = "Apagado deste PC; o registro fica no histórico")
    DELETED("Excluído");

    private static final Map<VolumeStatus, Set<VolumeStatus>> TRANSITIONS;

    static {
        EnumMap<VolumeStatus, Set<VolumeStatus>> map = new EnumMap<>(VolumeStatus.class);
        map.put(AVAILABLE, Set.of(ATTACHING, DELETED));
        map.put(ATTACHING, Set.of(ATTACHED, WAITING, FAILED));
        map.put(ATTACHED, Set.of(WAITING, DETACHING, ATTACHING, FAILED));
        map.put(WAITING, Set.of(ATTACHED, ATTACHING, DETACHING, FAILED));
        map.put(DETACHING, Set.of(AVAILABLE, ATTACHED, FAILED));
        map.put(FAILED, Set.of(ATTACHING, ATTACHED, WAITING, DETACHING, AVAILABLE, DELETED));
        map.put(DELETED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    VolumeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(VolumeStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(VolumeStatus current, VolumeStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação do disco inválida: " + current + " -> " + target);
        }
    }
}
