package com.bancada.specification;

import com.bancada.enums.CustomerStatus;
import com.bancada.models.Customer;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecification {

    private CustomerSpecification() {
    }

    public static Specification<Customer> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
            builder.like(builder.lower(root.get("name")), like),
            builder.like(builder.lower(root.get("email")), like),
            builder.like(builder.lower(root.get("document")), like));
    }

    /** Without an explicit filter, closed accounts stay out of the list. */
    public static Specification<Customer> statusIn(List<CustomerStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, builder) -> builder.notEqual(root.get("status"), CustomerStatus.CLOSED);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }
}
