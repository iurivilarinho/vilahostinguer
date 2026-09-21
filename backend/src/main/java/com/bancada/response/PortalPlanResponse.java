package com.bancada.response;

import com.bancada.enums.BillingCycle;
import com.bancada.models.Plan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;

@Schema(description = "Plano na vitrine do painel do cliente")
public record PortalPlanResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome", example = "VPS 1")
    String name,

    @Schema(description = "Descrição", example = "Para sites e APIs pequenas")
    String description,

    @Schema(description = "CPUs", example = "1")
    Double cpuLimit,

    @Schema(description = "Memória em MB", example = "512")
    Integer memoryMb,

    @Schema(description = "Disco em GB", example = "10")
    Integer diskGb,

    @Schema(description = "Backups guardados", example = "3")
    int backupSlots,

    @Schema(description = "Preços por período")
    List<PlanPriceResponse> prices,

    @Schema(description = "Destaque (\"Mais popular\")")
    boolean featured,

    @Schema(description = "Pode ser contratado agora (falso = esgotado)")
    boolean available,

    @Schema(description = "Sistemas operacionais disponíveis")
    List<DistributionResponse> distributions
) {

    public PortalPlanResponse(Plan plan, boolean available, List<DistributionResponse> distributions) {
        this(plan.getId(), plan.getName(), plan.getDescription(), plan.getCpuLimit(), plan.getMemoryMb(), plan.getDiskGb(),
            plan.getBackupSlots(), Arrays.stream(BillingCycle.values()).map(cycle -> new PlanPriceResponse(cycle, plan.getPriceMonthly())).toList(),
            plan.isFeatured(), available, distributions);
    }
}
