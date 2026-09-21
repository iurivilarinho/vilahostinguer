package com.bancada.response;

import com.bancada.enums.BillingCycle;
import com.bancada.models.Plan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Schema(description = "Plano de servidor (visão do administrador)")
public record PlanResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome", example = "VPS 1")
    String name,

    @Schema(description = "Descrição", example = "Para sites e APIs pequenas")
    String description,

    @Schema(description = "Dispositivo onde os servidores são criados")
    DeviceBasicResponse device,

    @Schema(description = "CPUs", example = "1")
    Double cpuLimit,

    @Schema(description = "Memória em MB", example = "512")
    Integer memoryMb,

    @Schema(description = "Disco anunciado em GB", example = "10")
    Integer diskGb,

    @Schema(description = "Backups guardados por servidor", example = "3")
    int backupSlots,

    @Schema(description = "Preço mensal", example = "19.90")
    BigDecimal priceMonthly,

    @Schema(description = "Preços por período")
    List<PlanPriceResponse> prices,

    @Schema(description = "Aparece na vitrine")
    boolean active,

    @Schema(description = "Destaque")
    boolean featured,

    @Schema(description = "Ordem na vitrine", example = "1")
    int orderNumber,

    @Schema(description = "Há recursos livres no dispositivo para mais um servidor")
    boolean available,

    @Schema(description = "Assinaturas já feitas deste plano", example = "4")
    long subscriptionCount,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public PlanResponse(Plan plan, boolean available, long subscriptionCount) {
        this(plan.getId(), plan.getName(), plan.getDescription(), new DeviceBasicResponse(plan.getDevice()), plan.getCpuLimit(),
            plan.getMemoryMb(), plan.getDiskGb(), plan.getBackupSlots(), plan.getPriceMonthly(),
            Arrays.stream(BillingCycle.values()).map(cycle -> new PlanPriceResponse(cycle, plan.getPriceMonthly())).toList(),
            plan.isActive(), plan.isFeatured(), plan.getOrderNumber(), available, subscriptionCount, plan.getCreatedAt(), plan.getUpdatedAt());
    }
}
