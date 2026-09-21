package com.bancada.specification;

import com.bancada.models.Plan;
import org.springframework.data.jpa.domain.Specification;

public final class PlanSpecification {

    private PlanSpecification() {
    }

    public static Specification<Plan> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("name")), like);
    }

    public static Specification<Plan> active(Boolean active) {
        if (active == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("active"), active);
    }

}
