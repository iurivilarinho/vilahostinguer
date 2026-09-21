package com.bancada.specification;

import com.bancada.enums.BackupStatus;
import com.bancada.models.Backup;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class BackupSpecification {

    private BackupSpecification() {
    }

    public static Specification<Backup> device(Long deviceId) {
        if (deviceId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("device").get("id"), deviceId);
    }

    public static Specification<Backup> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("name")), like);
    }

    public static Specification<Backup> statusIn(List<BackupStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }
}
