package com.bancada.specification;

import com.bancada.enums.OperationStatus;
import com.bancada.enums.OperationType;
import com.bancada.models.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class OperationSpecification {

    private OperationSpecification() {
    }

    public static Specification<Operation> device(Long deviceId) {
        if (deviceId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("device").get("id"), deviceId);
    }

    public static Specification<Operation> typeIn(List<OperationType> types) {
        if (types == null || types.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("type").in(types);
    }

    public static Specification<Operation> statusIn(List<OperationStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }

    public static Specification<Operation> createdBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> {
            if (startDate != null && endDate != null) {
                return builder.between(root.get("createdAt"), startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
            }
            if (startDate != null) {
                return builder.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay());
            }
            return builder.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay());
        };
    }
}
