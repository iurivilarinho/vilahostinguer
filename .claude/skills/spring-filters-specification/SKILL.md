---
name: spring-filters-specification
description: Build or update dynamic filtering with Spring Data JPA Specification. Use when Codex needs optional query parameters, date ranges, status filters, text search across fields, joins, pageable listing endpoints, or a single `Specification` class whose methods internally ignore null or empty inputs and are combined directly inside the service layer.
---

# Spring Specification Pattern

Implement filtered search with a filter object, a single `Specification` class, and service-side composition.

## Read first

- One or more `Specification` classes already present in the codebase
- The listing controller for the target entity
- The target repository
- The module's `filter/` package (or create it)
- `references/filter-patterns.md`

## Structure

- One `*Filter` class per listing/search endpoint, in the module's `filter/` package. It holds the optional criteria as **fields** (attributes), each with `@Schema` and the correct `java.time` type. See the filter-object rule in `spring-backend-standards`.
- One `Specification` class per aggregate or resource being filtered.
- The controller signature is `(EntityFilter filter, Pageable pageable)` — Spring binds the query params onto the filter fields. Do **not** spell out a long `@RequestParam` list in the method signature.

```java
@Schema(description = "Filtros de busca de leilões")
public class AuctionFilter {

    @Schema(description = "Texto de busca por título")
    private String search;

    @Schema(description = "Status do leilão")
    private List<AuctionStatus> status;

    @Schema(description = "Data inicial (inclusiva)", example = "2026-07-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "Data final (inclusiva)", example = "2026-07-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // getters/setters (no Lombok)
}
```

## Composition rules

- The service receives the `*Filter` object and reads its fields to build the combined specification. Do not reach into scattered `@RequestParam` values.
- Build the combined specification directly inside the service layer.
- Start from `Specification.where(...)` or `Specification.allOf(...)`, depending on the codebase preference.
- Chain `.and(...)` and `.or(...)` in the service with the static methods exposed by the `Specification` class.
- Prefer the direct repository call pattern: `repository.findAll(spec, pageable)` returning `Page<Entity>`, then map in the controller with `.map(EntityResponse::new)`.
- Keep one service method for `Page<T>` and another for `List<T>` when reports/export need the same filter without pagination.
- Never paginate by hand — always `Pageable` in, `Page<T>` out (see the pagination rule in `spring-backend-standards`).
- Delegate execution to the repository.

## Specification class rules

- Use static methods returning `Specification<Entity>`.
- Keep each predicate isolated in a small method.
- Each method must internally check whether the incoming parameter is null, blank, or empty.
- When a filter should not be applied, return `Specification.unrestricted()`.
- Use joins only where required.
- Use `builder.between` for date ranges.
- Use `builder.like(builder.lower(...), "%term%")` for case-insensitive string matching.
- Keep `searchAllFields` limited to basic string attributes unless the task explicitly needs relationship traversal.
- For generic search across associated entities or `@ElementCollection`, prefer metamodel-driven traversal only when the codebase already accepts that level of complexity.

## Controller rules

- Accept the filter object plus `Pageable`: `public ResponseEntity<Page<EntityResponse>> list(EntityFilter filter, Pageable pageable)`. No long `@RequestParam` list in the signature.
- Put `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` / `...ISO.DATE_TIME)` on the filter **fields**, choosing the type by meaning (`LocalDate` vs `LocalDateTime` vs `OffsetDateTime`).
- Accept lists for multi-status and multi-sector filters (as `List<...>` fields on the filter object).
- Pass `Pageable` straight through to the service; map the resulting `Page<Entity>` to `Page<EntityResponse>`.
- The filter object carries `@Schema` on the class and every field.

## Constraints

- Keep field names aligned across filter object, service, and specification.
- Do not create a separate `Filters` service just to assemble specifications.
- Do not embed filter logic directly in controllers or repositories.
- Do not place the `*Filter` class inside `service/`, `controller/`, or `specification/` — it lives in `filter/`.
- Prefer English names for fields, variables, and methods. Keep Portuguese only for Swagger/OpenAPI descriptions meant for users.
