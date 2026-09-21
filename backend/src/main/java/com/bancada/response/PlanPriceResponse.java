package com.bancada.response;

import com.bancada.enums.BillingCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Schema(description = "Preço de um plano num período de cobrança")
public record PlanPriceResponse(

    @Schema(description = "Período")
    BillingCycle cycle,

    @Schema(description = "Nome do período", example = "Anual")
    String cycleDescription,

    @Schema(description = "Meses do período", example = "12")
    int months,

    @Schema(description = "Desconto em relação ao mensal, em %", example = "20")
    int discountPercent,

    @Schema(description = "Valor do período inteiro", example = "191.04")
    BigDecimal total,

    @Schema(description = "Quanto sai por mês", example = "15.92")
    BigDecimal perMonth
) {

    public PlanPriceResponse(BillingCycle cycle, BigDecimal monthlyPrice) {
        this(cycle, cycle.getDescription(), cycle.getMonths(), cycle.getDiscountPercent(), cycle.priceFor(monthlyPrice),
            cycle.priceFor(monthlyPrice).divide(BigDecimal.valueOf(cycle.getMonths()), 2, RoundingMode.HALF_UP));
    }
}
