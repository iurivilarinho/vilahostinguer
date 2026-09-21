---
name: spring-backend-standards
description: Apply shared implementation standards for Java Spring backends. Use when Codex needs to create or update modules while following naming conventions, request and response mapping, constructor-based injection, Swagger documentation, repository rules, JPA foreign-key naming, status enums with transitions, filtered search, Excel exports, security, and standardized error handling.
---

# Spring Backend Standards

Use this skill as the top-level guide for backend work. Then load the narrower skill that matches the task.

## Non-negotiable rules (never violate these)

These are hard failures in review. They are not preferences.

1. **One public type per file, and no contract/model type nested in a service/controller/entity.** Never declare a secondary top-level class/record/enum in a file. Never nest a *contract or data-model* type — a request, response, basic-response, filter, query object, page wrapper, or an enum exposed in the API — inside a service, controller, or entity; extract it to its own file in the correct package (`request/`, `response/`, `filter/`, `enums/`, `records/`). A page wrapper like `LotsPage` or a query like `AuctionItemQuery` buried in a service is the smell this targets. (Two things are *fine*: a `private` nested helper that only encapsulates internal algorithm state/behaviour with no role in the API contract — e.g. an accumulator or counter used by one method — and a method-local `record`. Also fine: an `enum` that is *part of* a status-enum container, see `spring-status-enums`.)
2. **Never return `Map`, `HashMap`, `LinkedHashMap`, `List<Map<...>>`, `Object`, or untyped JSON from a controller or service public method.** Every payload — request, response, list item, page, even a one-field `{ "status": "ok" }` — is modeled by a real class in `request/`, `response/`, or `filter/`. Never build a response by hand with `Map<String, Object> body = new LinkedHashMap<>()`.
3. **Never paginate by hand.** No `subList`, no `from`/`to` index math, no `.skip()/.limit()`, no hand-built `hasNext`/`page`/`pageSize` maps. Listing endpoints accept `Pageable` and return `Page<EntityResponse>`. See "Pagination rules" below.
4. **Every class that holds attributes carries `@Schema`.** Requests, responses, basic responses, filters, and entities exposed in the contract get `@Schema` on the class and `@Schema(description = "...")` on every field. No exceptions for "small" DTOs. (Repositories, services, controllers, config, and pure utility/helper classes are not data-holding contract classes and do not need it.)
5. **Pick the date/time type by meaning** (see "Date and time type rules"): `LocalDate` for a date with no time, `LocalDateTime` for date+time with no zone, `OffsetDateTime` when the zone/offset matters. Never `java.util.Date`, `java.sql.Timestamp`, or `Calendar` in new code.
6. **Filters live in a `filter/` package as a filter object** — never as a long `@RequestParam` list in the controller signature. See "Filter object rules".
7. **The `request/` package exists and is used.** Input payloads are `EntityRequest` classes (or narrow `record` requests for PATCH), never a pile of loose `@RequestParam` or a `@RequestBody Map`. A module that has only `response/` and no `request/` is wrong.

## Package layout

A backend module has, at minimum:

```
models/        JPA entities
repository/    Spring Data interfaces
request/       *Request input classes (plus records/ for narrow updates)
response/      *Response and *BasicResponse output classes
filter/        *Filter objects for listing/search endpoints
service/       @Service business logic — ONLY services here, no DTOs/filters/enums/records
controller/    @RestController endpoints, thin
specification/ Specification classes for dynamic filtering
enums/         enums (status enums via spring-status-enums)
```

Do not park DTOs, filter classes, records, or enums inside `service/`. If you find one there, move it to the package above that matches its role.

## Naming and contract rules

- Use `EntityRequest` for input classes.
- Use `EntityResponse` for complete output models.
- Use `EntityBasicResponse` for compact or nested output models with basic information only.
- Do not use `form` or `dto` naming in new code unless the target codebase already forces that contract.
- Keep variable names, method names, class fields, and private helpers in English.
- Use Portuguese only in Swagger/OpenAPI descriptions meant to be shown to API consumers.
- Avoid `var`. Use explicit types unless inference is genuinely necessary to keep the code readable.
- Do not use Lombok under any circumstance. Do not generate `@Getter`, `@Setter`, `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, or any other Lombok annotation.
- Avoid excessive helper methods. Extract helpers only when they remove real duplication or isolate non-trivial logic cleanly.
- Document controllers, requests, responses, enums, and relevant models with Swagger/OpenAPI.

## Mapping rules

- Prefer `Entity(Request request)` constructors whenever possible.
- Prefer `EntityResponse(Entity entity)` and `EntityBasicResponse(Entity entity)` constructors whenever possible.
- Keep repetitive field mapping out of services when constructor mapping is sufficient.

## Dependency injection rules

- Use constructor injection in services and controllers.
- Annotate **every** Spring Data repository interface with `@Repository` from `org.springframework.stereotype` — no exceptions, even though Spring Data would auto-detect them. This is a mandatory project convention.

## Date and time type rules

Choose the type by what the value *means*, in entities, requests, responses, and filters alike:

- **`LocalDate`** — a calendar date with no time-of-day (birth date, due date, edital date, reference day). Format with `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)`.
- **`LocalDateTime`** — a date with a time-of-day where the zone is implicit/server-local (audit `createdAt`/`updatedAt`, scheduled-at, processed-at). Format with `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)`.
- **`OffsetDateTime`** — a timestamp where the zone/offset actually matters (events received from external systems across time zones, auction start/end exposed to clients in different regions, anything compared across zones). Format with `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)`.
- Never use `java.util.Date`, `java.sql.Timestamp`, `java.sql.Date`, or `java.util.Calendar` in new code. If you touch a class that still uses them, migrate the field to the correct `java.time` type.
- A field named like a pure date (`data`, `dataEdital`, `vencimento`, `*Date`) with `LocalDateTime` and a `00:00` time is a smell — it should almost always be `LocalDate`.

## Persistence naming rules

- Use `snake_case` for table names and non-foreign-key column names in `@Table(name = ...)` and `@Column(name = ...)`.
- For foreign keys, use `fk_Id_NomeDaColuna` in `@JoinColumn(name = ...)`.
- For foreign key constraint names, use `FK_FROM_TBTABELAORIGEM_FOR_TBTABELADESTINO`.
- Do not replace the existing foreign-key naming pattern with `snake_case`.

## Entity audit rules

- When the entity supports both creation and update timestamps, add `created_at` and `updated_at` fields with `LocalDateTime`, plus `@PrePersist` and `@PreUpdate`.
- When the entity is immutable after creation or has no update flow, add only `created_at` with `LocalDateTime` plus `@PrePersist`.
- Keep audit field names in Java as `createdAt` and `updatedAt`.
- When a user moves the entity (stock, status, price, approval, assignment), timestamps alone are not enough: add `createdBy`/`updatedBy` and record the movement history. Use `spring-audit-log`.

```java
@Column(name = "created_at", updatable = false, nullable = false)
private LocalDateTime createdAt;

@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;

@PrePersist
private void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
}

@PreUpdate
private void preUpdate() {
    this.updatedAt = LocalDateTime.now();
}
```

```java
@Column(name = "created_at", updatable = false, nullable = false)
private LocalDateTime createdAt;

@PrePersist
private void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
}
```

## API and springdoc rules

- When touching Swagger/OpenAPI configuration, inspect the existing springdoc config first instead of assuming it is correct.
- Verify that the configured title, description, and grouped API metadata match the current application. Fix copied text from other projects when it is incoherent.
- When `springdoc.swagger-ui.tagsSorter=alpha` is missing, add it.
- When `springdoc.swagger-ui.operationsSorter=alpha` is missing, add it.
- When OpenAPI metadata has no version, add one starting at `1`.
- Preserve existing config style and file placement when updating `application.yml`, `application.properties`, or Java-based OpenAPI config.

## Controller and service pattern

- Reuse the project's existing controller and service structure when it already matches this pattern.
- Add the pattern below only when the target module does not already provide an equivalent structure.
- When loading by id for normal entity retrieval, call the service `findById(...)` instead of accessing the repository directly from controllers or from another service layer shortcut.
- Before adding a repository lookup by id, first check whether the target entity already has a service with `findById(...)` or an equivalent ready method and reuse it.
- Use direct repository access for id lookups only when the task explicitly needs a different projection, lock mode, existence check, or query shape.

```java
@RestController
@RequestMapping("/resource-name")
@Validated
public class XxxController {

    private final XxxService xxxService;

    public XxxController(XxxService xxxService) {
        this.xxxService = xxxService;
    }

    @Operation(summary = "...")
    @ApiResponse(responseCode = "200", description = "...")
    @GetMapping("/{id}")
    public ResponseEntity<XxxResponse> findById(@PathVariable Long id) {
        Xxx xxx = xxxService.findById(id);
        return ResponseEntity.ok(new XxxResponse(xxx));
    }
}
```

```java
@Service
public class XxxService {

    private final XxxRepository xxxRepository;

    public XxxService(XxxRepository xxxRepository) {
        this.xxxRepository = xxxRepository;
    }

    @Transactional(readOnly = true)
    public Xxx findById(Long id) {
        return xxxRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Xxx não encontrada para ID: " + id));
    }

    @Transactional
    public Xxx create(XxxRequest request) {
        Xxx xxx = new Xxx(request);
        return xxxRepository.save(xxx);
    }
}
```

- Response codes to prefer: `200 OK`, `201 CREATED`, `204 NO_CONTENT`, `404 NOT_FOUND`.
- For responses with body, prefer `ResponseEntity.ok(body)` or `ResponseEntity.status(201).body(body)`.
- For responses without body, prefer `ResponseEntity.noContent().build()`.
- Use `EntityNotFoundException` for not-found cases.
- Use `DataIntegrityViolationException` for business rule violations.

## Swagger / `@Schema` rules

- Every data-holding contract class (`*Request`, `*Response`, `*BasicResponse`, `*Filter`, and entities exposed in the API) carries `@Schema` at the class level and `@Schema(description = "...")` on every field. This is mandatory, not "when convenient".
- Field descriptions are Portuguese (consumer-facing). Add `example = "..."` where it helps the reader.
- Mark required fields with `@Schema(requiredMode = Schema.RequiredMode.REQUIRED)`; mark read-only output fields with `accessMode = Schema.AccessMode.READ_ONLY`.
- Enums exposed in the contract document their values (see `spring-status-enums`).
- A new `*Request`/`*Response`/`*Filter` with no `@Schema` is incomplete — do not consider the task done.

```java
@Schema(description = "Dados de criação de um leilão")
public class AuctionRequest {

    @Schema(description = "Título do leilão", example = "Leilão de veículos GO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Data de realização do leilão (sem hora)", example = "2026-07-15")
    private LocalDate auctionDate;
}
```

## Typed responses (no `Map`)

- Controllers and service public methods return modeled types, never `Map`/`Object`/raw JSON.
- A status/ack response is a tiny named class, e.g. `SyncResultResponse { boolean success; int imported; String message; }` — not `Map.of("success", true, ...)`.
- A list response is `List<EntityResponse>` or `Page<EntityResponse>`, never `List<Map<String, Object>>`.
- Keep the JSON field names identical when replacing an existing `Map`-based payload, so you do not break the frontend contract: name the response fields exactly as the old map keys.
- Build responses through `EntityResponse(Entity entity)` constructors, not field-by-field map puts.

## Pagination rules

- Use Spring Data pagination only: accept `Pageable` in the controller, return `Page<T>` from the service, and respond with `Page<EntityResponse>`.
- Never paginate manually: no `subList`, no `from`/`to` arithmetic, no `stream().skip().limit()`, no hand-built `{ items, page, pageSize, hasNext, total }` maps.
- Map the page content with `page.map(EntityResponse::new)` so the response is `Page<EntityResponse>` and pagination metadata (totalElements, totalPages, number, size) comes from Spring.
- For an in-memory/external list you cannot query (e.g. a third-party feed), still expose `Pageable` and build the page with `new PageImpl<>(content, pageable, total)` instead of slicing by hand.
- The repository extends `JpaSpecificationExecutor` and you call `repository.findAll(spec, pageable)`.

```java
@GetMapping
public ResponseEntity<Page<AuctionResponse>> list(AuctionFilter filter, Pageable pageable) {
    Page<AuctionResponse> page = auctionService.search(filter, pageable).map(AuctionResponse::new);
    return ResponseEntity.ok(page);
}
```

## Filter object rules

- Group every optional listing/search parameter into a single `EntityFilter` class in the module's `filter/` package. The fields are the filter criteria, as class attributes — not a long parameter list inside the controller method signature.
- The controller method takes the filter object plus `Pageable` (Spring binds query params onto the filter fields automatically). It does not declare ten `@RequestParam`s.
- The filter class carries `@Schema` (it is part of the contract) and uses the correct `java.time` types with `@DateTimeFormat`.
- The service receives the filter object and builds the `Specification` from it (see `spring-filters-specification`). Do not reach into `@RequestParam` values scattered across the controller.

```java
@Schema(description = "Filtros de busca de leilões")
public class AuctionFilter {

    @Schema(description = "Texto de busca por título/descrição")
    private String search;

    @Schema(description = "Data inicial do período (inclusiva)", example = "2026-07-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "Data final do período (inclusiva)", example = "2026-07-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // getters/setters (no Lombok)
}
```

## Performance and data access

- Default every read-only flow (reports, listings, lookups) to `@Transactional(readOnly = true)` so report extraction never holds write locks on the underlying tables. Do not use pessimistic locks (`@Lock(LockModeType.PESSIMISTIC_*)` or `SELECT ... FOR UPDATE`) when extracting any report — report reads must not block writers.
- Resolve N+1 problems with `@EntityGraph` (preferred, declare the fetch paths on the repository method) or Hibernate `@BatchSize` on the association/collection. Reach for these before writing JPQL.
- Avoid `@Query`. Use it only when `@EntityGraph`, `@BatchSize`, derived query methods, and `Specification` cannot express the need.
- When a report is too complex or too slow to build by loading full entity graphs, use a dedicated projection (interface/record DTO projection) or a purpose-built repository/projection class that selects only the columns needed, instead of materializing whole entities.

## Skill routing

- For CRUD module creation or refactor: use `spring-crud-module`.
- For dynamic filtered search: use `spring-filters-specification`.
- For Excel exports: use `spring-report-excel`.
- For JWT auth and stateless security: use `spring-security-jwt`.
- For global error handling and request logging: use `spring-error-logging`.
- For status enums with transition rules: use `spring-status-enums`.
- For JUnit and Mockito tests: use `spring-tests-junit-mockito`.
- For `@Lob String` storage of Markdown or long text content: use `spring-md-content-storage`.
- For binary document/image/video entity storage with database-specific mapping: use `spring-document-storage`.
- For recording who moved an entity, when, and what changed (createdBy/updatedBy stamping, audit log, movement history): use `spring-audit-log`.
