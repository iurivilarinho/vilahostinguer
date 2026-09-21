package com.bancada.response;

import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Servidor do cliente (visão do painel do cliente)")
public record ServerResponse(

    @Schema(description = "Identificador (o da assinatura)", example = "3")
    Long id,

    @Schema(description = "Nome", example = "meu-site")
    String hostname,

    @Schema(description = "Plano", example = "VPS 1")
    String planName,

    @Schema(description = "CPUs", example = "1")
    Double cpuLimit,

    @Schema(description = "Memória em MB", example = "512")
    Integer memoryMb,

    @Schema(description = "Disco em GB", example = "10")
    Integer diskGb,

    @Schema(description = "Backups guardados", example = "3")
    int backupSlots,

    @Schema(description = "Sistema")
    MachineDistribution distribution,

    @Schema(description = "Nome do sistema", example = "Ubuntu")
    String distributionName,

    @Schema(description = "Versão", example = "24.04")
    String version,

    @Schema(description = "Usuário com sudo", example = "maria")
    String username,

    @Schema(description = "Situação da assinatura")
    SubscriptionStatus subscriptionStatus,

    @Schema(description = "Descrição da situação da assinatura", example = "Ativa")
    String subscriptionStatusDescription,

    @Schema(description = "Situação da máquina (vazio antes de existir)")
    MachineStatus machineStatus,

    @Schema(description = "Descrição da situação da máquina", example = "Ligada")
    String machineStatusDescription,

    @Schema(description = "Endereço para o SSH", example = "ssh.vilahost.com.br")
    String sshHost,

    @Schema(description = "Porta do SSH", example = "20000")
    Integer sshPort,

    @Schema(description = "Comando de acesso", example = "ssh maria@ssh.vilahost.com.br -p 20000")
    String sshCommand,

    @Schema(description = "Endereço do site", example = "meu-site.clientes.vilahost.com.br")
    String siteHostname,

    @Schema(description = "Próximo vencimento", example = "2026-10-21")
    LocalDate nextDueDate,

    @Schema(description = "Encerra no fim do período pago")
    boolean cancelAtPeriodEnd,

    @Schema(description = "Aviso para o cliente (servidor sendo criado, suspenso por atraso...)")
    String notice
) {
}
