package com.bancada.specification;

import com.bancada.enums.AuditAction;
import com.bancada.models.AuditLog;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> entityType(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> entityId(Long entityId) {
        if (entityId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> user(Long userId) {
        if (userId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("userId"), userId);
    }

    public static Specification<AuditLog> actionIn(List<AuditAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("action").in(actions);
    }

    public static Specification<AuditLog> between(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return Specification.where(null);
        }
        if (start == null) {
            return (root, query, builder) -> builder.lessThan(root.get("occurredAt"), end.plusDays(1).atStartOfDay());
        }
        if (end == null) {
            return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("occurredAt"), start.atStartOfDay());
        }
        return (root, query, builder) -> builder.between(root.get("occurredAt"), start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }
}
