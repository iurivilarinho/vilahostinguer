package com.bancada.specification;

import com.bancada.enums.DeviceStatus;
import com.bancada.models.Device;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class DeviceSpecification {

    private DeviceSpecification() {
    }

    public static Specification<Device> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
            builder.like(builder.lower(root.get("name")), like),
            builder.like(builder.lower(root.get("host")), like),
            builder.like(builder.lower(builder.coalesce(root.get("hostname"), "")), like),
            builder.like(builder.lower(builder.coalesce(root.get("osName"), "")), like),
            builder.like(builder.lower(builder.coalesce(root.get("model"), "")), like));
    }

    public static Specification<Device> statusIn(List<DeviceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }

    public static Specification<Device> online(Boolean online) {
        if (online == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("online"), online);
    }

    public static Specification<Device> active(Boolean active) {
        if (active == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("active"), active);
    }
}
