package com.bancada.specification;

import com.bancada.models.Credential;
import org.springframework.data.jpa.domain.Specification;

public final class CredentialSpecification {

    private CredentialSpecification() {
    }

    public static Specification<Credential> search(String term) {
        if (term == null || term.isBlank()) {
            return Specification.where(null);
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
            builder.like(builder.lower(root.get("name")), like),
            builder.like(builder.lower(root.get("username")), like));
    }

    public static Specification<Credential> active(Boolean active) {
        if (active == null) {
            return Specification.where(null);
        }
        return (root, query, builder) -> builder.equal(root.get("active"), active);
    }
}
