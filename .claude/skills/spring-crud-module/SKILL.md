---
name: spring-crud-module
description: Create or change Spring Boot CRUD modules following a layered architecture with entity, repository, service, controller, request, response, and optional records for narrow updates. Use when Codex needs to add or update domain models, REST endpoints, business rules, file upload flows, ordering logic, aggregate relationships, Swagger-documented contracts, or response mapping patterns in a Java Spring backend.
---

# Spring CRUD Module

Follow a reusable layered module pattern for Spring Boot backends.

## Read first

Read these files before editing:

- The primary entity for the feature
- The matching repository, service, and controller
- The input models used for create and update operations
- The response classes or view models already used by sibling modules
- `references/module-checklist.md`

## Implement a new module

Create or update files in this order:

1. `models/`: define the JPA entity and relationships.
2. `repository/`: expose Spring Data access.
3. `request/` and `records/`: model input payloads. This package is mandatory — input is never a loose `@RequestParam` pile or a `@RequestBody Map`.
4. `response/`: shape output payloads. Never return `Map`/`Object`/raw JSON; model a `*Response` class even for one-field acks.
5. `filter/`: model listing/search criteria as a `*Filter` object (see `spring-filters-specification`).
6. `service/`: keep all business rules here. ONLY `@Service` classes live here — no DTOs, filters, enums, or records.
7. `controller/`: keep endpoints thin and delegate.

Each top-level type lives in its own file in the package that matches its role. Never nest a helper `record`/`class`/`enum` inside a service, controller, or entity, and never put two top-level types in one file. See the non-negotiable rules in `spring-backend-standards`.

## Entity rules

- Use JPA annotations directly on fields.
- Prefer `@ManyToOne`, `@OneToMany`, `@ManyToMany`, `@ElementCollection`, and `@Enumerated(EnumType.STRING)` exactly as the project already does.
- Keep primitive domain data in the entity, including dates, booleans, notes, ordering, and foreign-key-like user ids.
- Whenever possible, add a constructor that maps from the corresponding `request` object so the service does not set fields one by one.
- Keep `equals` and `hashCode` based on `id`.
- Use `@JsonIgnore` or `@JsonIgnoreProperties` only when needed to avoid serialization recursion or Hibernate proxy noise.
- Document entities with Swagger annotations when that matches the project's contract style.
- Do not use Lombok under any circumstance.
- Use `snake_case` for `@Table(name = ...)` and `@Column(name = ...)` except in foreign-key columns that already follow the project's `fk_Id_NomeDaColuna` pattern.
- For foreign keys, use `@JoinColumn(name = "fk_Id_NomeDaColuna", foreignKey = @ForeignKey(name = "FK_FROM_TBTABELAORIGEM_FOR_TBTABELADESTINO"))`.
- Add entity audit fields with `LocalDateTime`:
  - Use `created_at` and `updated_at` plus `@PrePersist` and `@PreUpdate` for entities that can be updated.
  - Use only `created_at` plus `@PrePersist` for entities that are immutable after creation.
- For Markdown or other large textual file content, use `@Lob private String content;`.
- For binary upload entities, route to `spring-document-storage` and use `@Table(name = "tbDocument")`.
- Keep identifiers and method names in English. Use Portuguese only in Swagger/OpenAPI descriptions exposed to users.

## Controller rules

- Use `@RestController` and `@RequestMapping`.
- Add `@Validated` when the controller validates path, query, or request-body constraints.
- Inject dependencies through the constructor.
- Return `ResponseEntity`.
- Keep validation in request objects with `@Valid`.
- Use `@RequestPart` for multipart forms and files.
- Keep controller logic orchestration-only. Do not move business rules here, and do not add private helper methods that build payloads, map data, or do calculations — that belongs in the service or in a response constructor.
- Never return `Map`/`HashMap`/`LinkedHashMap`/`List<Map<...>>`/`Object` — return a modeled `*Response`, `List<*Response>`, or `Page<*Response>`.
- List endpoints accept `Pageable` and return `Page<*Response>`; never paginate by hand. Search endpoints accept a `*Filter` object plus `Pageable`.
- Document controllers, endpoints, parameters, requests, and responses with Swagger/OpenAPI annotations.
- Prefer `200 OK`, `201 CREATED`, `204 NO_CONTENT`, and `404 NOT_FOUND`.
- Use `ResponseEntity.ok(body)` for success with body.
- Use `ResponseEntity.status(201).body(body)` for create endpoints that return a body.
- Use `ResponseEntity.noContent().build()` for success without body.
- When retrieving a single entity by id, call `service.findById(id)` and map the returned entity into `EntityResponse`.
- If the module does not already follow an equivalent pattern, introduce the controller structure below.

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

## Service rules

- Annotate with `@Service`.
- Use `@Transactional(readOnly = true)` for reads and `@Transactional` for writes.
- Resolve dependencies inside the service layer.
- Throw `EntityNotFoundException` for missing resources so the global handler formats the response.
- Use `DataIntegrityViolationException` for business rule violations when the task calls for that exception pattern.
- Preserve aggregate consistency in update flows: clear collections, convert request objects, flush only when needed, then repopulate.
- Keep ordering logic in service methods when entities support `orderNumber`.
- Use constructor injection only.
- Prefer entity constructors and focused update methods instead of repetitive setter blocks in services.
- Avoid `var` unless the explicit type would make the statement materially harder to read.
- Avoid scattering small helper methods when straight-line service code is clearer.
- For standard entity retrieval by id, expose and reuse a `findById(Long id)` service method instead of redoing repository lookups across controllers and sibling services.
- If the module does not already provide an equivalent pattern, introduce the service structure below.

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

## Repository rules

- Extend `JpaRepository<..., Long>`.
- Add `JpaSpecificationExecutor` when the entity supports filtered searches.
- Prefer derived query methods first.
- Use `@Query` only for small custom aggregate queries such as max order number.
- Annotate every repository interface with `@Repository` from `org.springframework.stereotype` — mandatory project convention, no exceptions.

## Output rules

- Use `EntityRequest` for input contracts.
- Use `EntityResponse` for complete output contracts.
- Use `EntityBasicResponse` for lightweight nested or list-oriented contracts when only basic information is needed.
- Resolve external integrations only in service/response layers, not in entities.
- Use small `record` requests for narrow PATCH updates (each in its own file under `records/`).
- Whenever possible, add `EntityResponse(Entity entity)` or `EntityBasicResponse(Entity entity)` constructors so mapping does not stay spread across services/controllers.
- Never expose `Map`/`Object`/raw JSON as a contract — model every payload, including status/ack responses (`SyncResultResponse`, etc.) and list items.
- `@Schema` is mandatory on every `*Request`/`*Response`/`*BasicResponse`/`*Filter`: on the class and on every field. Use the correct `java.time` type per field (`LocalDate`/`LocalDateTime`/`OffsetDateTime`). A model without `@Schema` is not done.

## Springdoc rules

- When the task touches API documentation, inspect the current springdoc configuration file.
- Add `springdoc.swagger-ui.tagsSorter=alpha` when missing.
- Add `springdoc.swagger-ui.operationsSorter=alpha` when missing.
- Fix incoherent copied application titles or descriptions when they do not match the current system.
- When no API version is configured, start with version `1`.

## Constraints

- Preserve the repository naming and folder conventions already present in the target codebase.
- Match folder placement exactly.
- Reuse the same endpoint style already present in sibling modules.
