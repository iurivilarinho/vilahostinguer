package com.bancada.specification;

import com.bancada.enums.DnsProvider;
import com.bancada.models.Domain;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class DomainSpecification {

    private DomainSpecification() {
    }

    public static Specification<Domain> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("name")), like);
    }

    public static Specification<Domain> providerIn(List<DnsProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("provider").in(providers);
    }

    /** Without an explicit filter, archived domains stay out of the list. */
    public static Specification<Domain> active(Boolean active) {
        boolean wanted = active == null || active;
        return (root, query, builder) -> builder.equal(root.get("active"), wanted);
    }
}
