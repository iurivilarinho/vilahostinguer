package com.bancada.records;

import com.bancada.models.Plan;
import java.math.BigDecimal;

/** What the audit log keeps of a plan: the offer as it was sold. */
public record PlanSnapshot(String name, Long deviceId, Double cpuLimit, Integer memoryMb, Integer diskGb, int backupSlots,
                           BigDecimal priceMonthly, boolean active) {

    public PlanSnapshot(Plan plan) {
        this(plan.getName(), plan.getDevice().getId(), plan.getCpuLimit(), plan.getMemoryMb(), plan.getDiskGb(), plan.getBackupSlots(),
            plan.getPriceMonthly(), plan.isActive());
    }
}
