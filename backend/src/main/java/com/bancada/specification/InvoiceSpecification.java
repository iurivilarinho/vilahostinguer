package com.bancada.specification;

import com.bancada.enums.InvoiceStatus;
import com.bancada.models.Invoice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class InvoiceSpecification {

    private InvoiceSpecification() {
    }

    public static Specification<Invoice> customer(Long customerId) {
        if (customerId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Invoice> subscription(Long subscriptionId) {
        if (subscriptionId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("subscription").get("id"), subscriptionId);
    }

    public static Specification<Invoice> statusIn(List<InvoiceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }

    public static Specification<Invoice> overdue(Boolean overdue, LocalDate today) {
        if (overdue == null || !overdue) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.and(
            builder.equal(root.get("status"), InvoiceStatus.OPEN),
            builder.lessThan(root.get("dueDate"), today));
    }

    public static Specification<Invoice> dueBetween(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return Specification.where(null);
        }
        if (from == null) {
            return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("dueDate"), to);
        }
        if (to == null) {
            return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("dueDate"), from);
        }
        return (root, query, builder) -> builder.between(root.get("dueDate"), from, to);
    }
}
