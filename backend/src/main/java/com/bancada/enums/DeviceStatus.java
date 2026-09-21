package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação de acesso do painel ao dispositivo")
public enum DeviceStatus {

    @Schema(description = "Detectado — o SSH respondeu, mas ainda não há credencial que funcione")
    DISCOVERED("Detectado"),

    @Schema(description = "Pronto — o painel entra no dispositivo com a credencial vinculada")
    READY("Pronto"),

    @Schema(description = "Falha de acesso — a credencial vinculada foi recusada")
    AUTH_FAILED("Falha de acesso");

    private static final Map<DeviceStatus, Set<DeviceStatus>> TRANSITIONS;

    static {
        EnumMap<DeviceStatus, Set<DeviceStatus>> map = new EnumMap<>(DeviceStatus.class);
        map.put(DISCOVERED, Set.of(READY, AUTH_FAILED));
        map.put(READY, Set.of(AUTH_FAILED, DISCOVERED));
        map.put(AUTH_FAILED, Set.of(READY, DISCOVERED));
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    DeviceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(DeviceStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(DeviceStatus current, DeviceStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação do dispositivo inválida: " + current + " -> " + target);
        }
    }
}
