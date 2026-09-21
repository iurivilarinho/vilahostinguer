package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Cadastro ou alteração de plano")
public record PlanRequest(

    @Schema(description = "Nome", example = "VPS 1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome")
    @Size(max = 60, message = "Até 60 caracteres")
    String name,

    @Schema(description = "Descrição curta mostrada na vitrine", example = "Para sites e APIs pequenas")
    @Size(max = 300, message = "Até 300 caracteres")
    String description,

    @Schema(description = "Dispositivo onde os servidores deste plano são criados", example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha o dispositivo")
    Long deviceId,

    @Schema(description = "CPUs", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe as CPUs")
    @DecimalMin(value = "0.1", message = "Mínimo de 0,1 CPU")
    @DecimalMax(value = "64", message = "Máximo de 64 CPUs")
    Double cpuLimit,

    @Schema(description = "Memória em MB", example = "512", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a memória")
    @Min(value = 64, message = "Mínimo de 64 MB")
    @Max(value = 262_144, message = "Máximo de 256 GB")
    Integer memoryMb,

    @Schema(description = "Disco anunciado em GB (informativo)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe o disco")
    @Min(value = 1, message = "Mínimo de 1 GB")
    @Max(value = 100_000, message = "Máximo de 100000 GB")
    Integer diskGb,

    @Schema(description = "Backups guardados por servidor", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 0, message = "Mínimo de 0")
    @Max(value = 30, message = "Máximo de 30")
    int backupSlots,

    @Schema(description = "Preço mensal em reais", example = "19.90", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe o preço")
    @DecimalMin(value = "0.00", message = "Preço inválido")
    @Digits(integer = 7, fraction = 2, message = "Use até 2 casas decimais")
    BigDecimal priceMonthly,

    @Schema(description = "Aparece na vitrine", example = "true")
    boolean active,

    @Schema(description = "Destaque (\"Mais popular\")", example = "false")
    boolean featured,

    @Schema(description = "Ordem na vitrine", example = "1")
    @Min(value = 0, message = "Mínimo de 0")
    int orderNumber
) {
}
