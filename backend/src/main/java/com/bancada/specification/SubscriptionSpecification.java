package com.bancada.specification;

import com.bancada.enums.SubscriptionStatus;
import com.bancada.models.Subscription;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SubscriptionSpecification {

    private SubscriptionSpecification() {
    }

    public static Specification<Subscription> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
            builder.like(builder.lower(root.get("hostname")), like),
            builder.like(builder.lower(root.get("customer").get("name")), like),
            builder.like(builder.lower(root.get("customer").get("email")), like));
    }

    public static Specification<Subscription> customer(Long customerId) {
        if (customerId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Subscription> plan(Long planId) {
        if (planId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("plan").get("id"), planId);
    }

    public static Specification<Subscription> statusIn(List<SubscriptionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }
}
