package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Schema(description = "Período de cobrança de uma assinatura; períodos maiores têm desconto")
public enum BillingCycle {

    @Schema(description = "Mensal")
    MONTHLY("Mensal", 1, 0),

    @Schema(description = "Trimestral, 5% de desconto")
    QUARTERLY("Trimestral", 3, 5),

    @Schema(description = "Semestral, 10% de desconto")
    SEMIANNUAL("Semestral", 6, 10),

    @Schema(description = "Anual, 20% de desconto")
    ANNUAL("Anual", 12, 20);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final String description;
    private final int months;
    private final int discountPercent;

    BillingCycle(String description, int months, int discountPercent) {
        this.description = description;
        this.months = months;
        this.discountPercent = discountPercent;
    }

    /** Price of the whole period for a monthly price, with the cycle discount. */
    public BigDecimal priceFor(BigDecimal monthlyPrice) {
        return monthlyPrice.multiply(BigDecimal.valueOf(months))
            .multiply(HUNDRED.subtract(BigDecimal.valueOf(discountPercent)))
            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public String getDescription() {
        return description;
    }

    public int getMonths() {
        return months;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }
}
