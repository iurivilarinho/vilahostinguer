package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ação registrada na auditoria")
public enum AuditAction {

    @Schema(description = "Criação")
    CREATE("Criação"),

    @Schema(description = "Atualização")
    UPDATE("Atualização"),

    @Schema(description = "Mudança de situação")
    STATUS_CHANGE("Mudança de situação"),

    @Schema(description = "Pagamento confirmado")
    PAYMENT("Pagamento"),

    @Schema(description = "Servidor criado para uma assinatura")
    PROVISION("Criação do servidor"),

    @Schema(description = "Troca de senha (a senha não é registrada)")
    PASSWORD_CHANGE("Troca de senha"),

    @Schema(description = "Ligar, desligar ou reiniciar um servidor")
    SERVER_ACTION("Ação no servidor"),

    @Schema(description = "Reinstalação de servidor")
    REINSTALL("Reinstalação"),

    @Schema(description = "Backup de servidor")
    BACKUP("Backup"),

    @Schema(description = "Restauração de servidor")
    RESTORE("Restauração");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
