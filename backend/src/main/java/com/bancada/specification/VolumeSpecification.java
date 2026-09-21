package com.bancada.specification;

import com.bancada.enums.VolumeStatus;
import com.bancada.models.Volume;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class VolumeSpecification {

    private VolumeSpecification() {
    }

    public static Specification<Volume> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("name")), like);
    }

    /** Without an explicit filter, deleted disks stay out of the list. */
    public static Specification<Volume> statusIn(List<VolumeStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, builder) -> builder.notEqual(root.get("status"), VolumeStatus.DELETED);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }

    public static Specification<Volume> device(Long deviceId) {
        if (deviceId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("device").get("id"), deviceId);
    }

    public static Specification<Volume> machine(Long machineId) {
        if (machineId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("machine").get("id"), machineId);
    }

    public static Specification<Volume> drive(String drive) {
        if (drive == null || drive.isBlank()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(builder.upper(root.get("drive")), drive.trim().toUpperCase());
    }
}
