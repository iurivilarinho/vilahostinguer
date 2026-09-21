package com.bancada.specification;

import com.bancada.enums.MachineStatus;
import com.bancada.models.Machine;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class MachineSpecification {

    private MachineSpecification() {
    }

    public static Specification<Machine> device(Long deviceId) {
        if (deviceId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("device").get("id"), deviceId);
    }

    public static Specification<Machine> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("name")), like);
    }

    /** Without an explicit filter, removed machines stay out of the list. */
    public static Specification<Machine> statusIn(List<MachineStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, builder) -> builder.notEqual(root.get("status"), MachineStatus.REMOVED);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }
}
