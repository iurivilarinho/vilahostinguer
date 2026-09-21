package com.bancada.records;

import com.bancada.enums.BillingCycle;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.models.Subscription;
import java.math.BigDecimal;
import java.time.LocalDate;

/** What the audit log keeps of a subscription (never the pending password). */
public record SubscriptionSnapshot(SubscriptionStatus status, Long planId, BillingCycle cycle, BigDecimal price,
                                   MachineDistribution distribution, String version, LocalDate nextDueDate,
                                   boolean cancelAtPeriodEnd, Long machineId) {

    public SubscriptionSnapshot(Subscription subscription) {
        this(subscription.getStatus(), subscription.getPlan().getId(), subscription.getCycle(), subscription.getPrice(),
            subscription.getDistribution(), subscription.getVersion(), subscription.getNextDueDate(), subscription.isCancelAtPeriodEnd(),
            subscription.getMachine() == null ? null : subscription.getMachine().getId());
    }
}
