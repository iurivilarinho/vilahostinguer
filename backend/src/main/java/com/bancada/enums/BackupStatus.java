package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Schema(description = "Situação de um backup")
public enum BackupStatus {

    @Schema(description = "Sendo gerado")
    CREATING("Gerando"),

    @Schema(description = "Arquivo salvo neste computador e pronto para restaurar")
    AVAILABLE("Disponível"),

    @Schema(description = "A geração falhou")
    FAILED("Falhou"),

    @Schema(description = "Descartado — o arquivo foi apagado e o registro fica no histórico")
    DISCARDED("Descartado");

    private static final Map<BackupStatus, Set<BackupStatus>> TRANSITIONS;

    static {
        EnumMap<BackupStatus, Set<BackupStatus>> map = new EnumMap<>(BackupStatus.class);
        map.put(CREATING, Set.of(AVAILABLE, FAILED));
        map.put(AVAILABLE, Set.of(DISCARDED));
        map.put(FAILED, Set.of(DISCARDED));
        map.put(DISCARDED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final String description;

    BackupStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(BackupStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static void validateTransition(BackupStatus current, BackupStatus target) {
        if (current == target) {
            return;
        }
        if (current == null || target == null || !current.canTransitionTo(target)) {
            throw new IllegalStateException("Transição de situação do backup inválida: " + current + " -> " + target);
        }
    }
}
