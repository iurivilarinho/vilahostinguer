package com.bancada.specification;

import com.bancada.enums.RouteStatus;
import com.bancada.enums.RouteType;
import com.bancada.models.Route;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class RouteSpecification {

    private RouteSpecification() {
    }

    public static Specification<Route> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
            builder.like(builder.lower(root.get("hostname")), like),
            builder.like(builder.lower(root.get("description")), like));
    }

    public static Specification<Route> typeIn(List<RouteType> types) {
        if (types == null || types.isEmpty()) {
            return Specification.where(null);
        }
        return (root, query, builder) -> root.get("type").in(types);
    }

    /** Without an explicit filter, removed routes stay out of the list. */
    public static Specification<Route> statusIn(List<RouteStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, builder) -> builder.notEqual(root.get("status"), RouteStatus.REMOVED);
        }
        return (root, query, builder) -> root.get("status").in(statuses);
    }

    public static Specification<Route> device(Long deviceId) {
        if (deviceId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("device").get("id"), deviceId);
    }

    public static Specification<Route> machine(Long machineId) {
        if (machineId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("machine").get("id"), machineId);
    }

    public static Specification<Route> domain(Long domainId) {
        if (domainId == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("domain").get("id"), domainId);
    }
}
