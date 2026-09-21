---
name: spring-audit-log
description: Record who changed an entity, when, and what moved, in Java Spring backends using entity stamping plus an append-only audit log. Use when Codex needs createdBy/updatedBy stamping, an AuditLog entity, an AuditAction enum, movement history (quantity, type, before/after values), a history endpoint, or resolving the logged user id for auditing.
---

# Spring Audit Log

Use this skill for any entity a **user moves** — an entity whose state a person changes and where "who did this, when, and what did it become?" is a question someone will eventually ask (stock, status, price, approval, assignment, balance). It is system-agnostic: the same two layers apply to every backend.

Do **not** use it for machine-only traces (integration retries, sync errors, webhook payloads, request timing). Those are infrastructure logs — see `spring-error-logging`.

## Read first

- The entity being changed, and its service (this is where audit calls belong)
- Any existing `AuditLog` / `Historico` / `*History` entity in the target backend — **reuse it, do not add a second one**
- How this backend resolves the logged user (`AuthenticatedUserService`, or `UserService.findUserLoggedIn()`) — see `spring-security-jwt`
- `references/audit-log-patterns.md`

## The two layers

Both are needed, and they answer different questions. Do not treat one as a substitute for the other.

| Layer | Answers | Lives on |
| --- | --- | --- |
| **1. Stamping** | "Who owns this row right now, and when was it last touched?" | The entity itself (4 columns) |
| **2. Audit log** | "Everything that ever happened to this row, in order, and what moved." | A separate append-only table |

Stamping is cheap and always current, but it only remembers the *last* writer. The audit log is the history. An entity with movement gets both.

## Layer 1 — entity stamping

Extend the `created_at` / `updated_at` rules in `spring-backend-standards` with the **actor**:

```java
@Column(name = "created_at", updatable = false, nullable = false)
private LocalDateTime createdAt;

@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;

@Column(name = "created_by", updatable = false)
private Long createdBy;

@Column(name = "updated_by")
private Long updatedBy;
```

- `createdBy` / `updatedBy` store the **raw user id as `Long`** — not a `@ManyToOne User`. See "Store the id, not the relation" below.
- Set the timestamps in `@PrePersist` / `@PreUpdate` exactly as `spring-backend-standards` shows.
- Set the **actor in the service**, not in the entity callback — the callback has no business asking Spring Security who is logged in, and a background job or import has no logged user at all.
- An entity that is immutable after creation gets only `createdAt` + `createdBy`.

## Layer 2 — the audit log entity

One `AuditLog` per backend, generic over every entity. Append-only: **no setters, every column `updatable = false`.** A row that can be edited is not an audit trail.

```java
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "IDX_AUDIT_ENTITY", columnList = "entity_type,entity_id"),
        @Index(name = "IDX_AUDIT_USER", columnList = "user_id"),
        @Index(name = "IDX_AUDIT_AT", columnList = "occurred_at") })
@Schema(description = "Registro imutável de uma movimentação feita por um usuário")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador do registro", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Entidade movimentada", example = "Product", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(name = "entity_type", length = 80, nullable = false, updatable = false)
    private String entityType;

    @Schema(description = "Identificador do registro movimentado", example = "42")
    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Schema(description = "Ação executada", requiredMode = Schema.RequiredMode.REQUIRED)
    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 30, nullable = false, updatable = false)
    private AuditAction action;

    @Schema(description = "Identificador do usuário que executou a ação", example = "7")
    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Schema(description = "Nome do usuário no momento da ação", example = "Maria Silva")
    @Column(name = "user_name", length = 120, updatable = false)
    private String userName;

    @Schema(description = "Data e hora da movimentação", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Schema(description = "Quantidade movimentada, quando a ação move volume", example = "12.00")
    @Column(name = "quantity", precision = 15, scale = 2, updatable = false)
    private BigDecimal quantity;

    @Schema(description = "Estado anterior em JSON", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "old_value", length = 2000, updatable = false)
    private String oldValue;

    @Schema(description = "Estado posterior em JSON", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "new_value", length = 2000, updatable = false)
    private String newValue;

    @Schema(description = "Justificativa informada pelo usuário", example = "Ajuste de inventário")
    @Column(name = "reason", length = 1000, updatable = false)
    private String reason;

    @Schema(description = "Origem da ação", example = "POST /products/42/stock")
    @Column(name = "source", length = 120, updatable = false)
    private String source;

    protected AuditLog() {
    }

    // constructor that takes every value; getters only, no setters

    @PrePersist
    private void prePersist() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }
}
```

Follow the target module's table/column naming when it already differs (`tbAuditLog` + camelCase columns is the existing shape in some backends) — `spring-backend-standards` governs naming, this skill governs content.

### Why these columns

- **`entityType` + `entityId`** — the generic key. This is what makes one table serve every module: never create `ProductAuditLog`, `OrderAuditLog`, etc.
- **`quantity`** gets its own typed column even though `newValue` already carries it, because it is the one field that gets **filtered, summed, and reported on**. Digging it out of a JSON blob in SQL is the thing you will regret.
- **`oldValue` / `newValue`** absorb everything else, generically. That is the point: a new module adds an `AuditAction` constant and changes nothing else.
- **`userName`** is a denormalized snapshot for display, so the history renders without joining users and still reads correctly after a rename. `userId` remains the source of truth.

## The action enum

```java
public enum AuditAction {
    CREATE("Criação"),
    UPDATE("Atualização"),
    DELETE("Exclusão"),
    STATUS_CHANGE("Mudança de status"),
    QUANTITY_CHANGE("Movimentação de quantidade"),
    EXPORT("Exportação");

    private final String description;
    // constructor + getter
}
```

- Always `@Enumerated(EnumType.STRING)`. Without it JPA persists the **ordinal**, and reordering the enum silently rewrites history — this bug is live in at least one backend today.
- Portuguese descriptions and Swagger-documented values, per `spring-status-enums`.
- Add domain constants (`APPROVE`, `REJECT`, `ENTRADA`, `SAIDA`) as the module needs them. Adding a constant is the normal way to extend this.

## The service

```java
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public AuditService(AuditLogRepository auditLogRepository, AuthenticatedUserService authenticatedUserService) {
        this.auditLogRepository = auditLogRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional
    public void record(AuditAction action, String entityType, Long entityId, Object oldValue, Object newValue) {
        // resolve the logged user, serialize the values, strip sensitive fields, save
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> history(String entityType, Long entityId, Pageable pageable) {
        // ...
    }
}
```

### Record in the caller's transaction

`record(...)` is `@Transactional` with the **default propagation**, so it joins the business transaction: if the operation rolls back, its audit row rolls back with it, and if the audit write fails, the operation fails.

This is deliberate, and the two existing backends disagree on it — one uses `REQUIRES_NEW` plus a `catch` that swallows the failure so the main operation is never affected. That trade is wrong for user movements: it buys availability by allowing the log to lie in both directions (a row describing a change that was rolled back, or a change with no row at all). An audit trail nobody can trust is worse than no audit trail, because people act on it.

Use `REQUIRES_NEW` **only** for actions that are genuinely outside the business transaction — a read/export you want logged even if the response later fails, or a purge job. Never as a blanket policy.

### Call it from the service, after the change

```java
@Transactional
public Product updateStock(Long id, StockRequest request) {
    Product product = findById(id);
    ProductSnapshot before = new ProductSnapshot(product);

    product.moveStock(request.getQuantity());
    Product saved = productRepository.save(product);

    auditService.record(AuditAction.QUANTITY_CHANGE, "Product", saved.getId(), before, new ProductSnapshot(saved));
    return saved;
}
```

- Capture `before` **prior** to mutating — a JPA entity is mutable and it will read back as the new state otherwise. This is the single most common way this pattern is implemented wrong.
- Never call the audit service from a controller (it does not know what changed) or from `@PrePersist`/`@PreUpdate` (entity callbacks must not touch the `EntityManager` or Spring Security).

### Store the id, not the relation

`userId` is a `Long` column, not `@ManyToOne User`. The log outlives the user: a deleted or deactivated user must not break history, and listing 500 rows must not lazily hydrate 500 `User` entities. `userName` covers the display need. Some existing code uses `@ManyToOne User` for movement history — leave it alone when you find it, but do not copy it into new code.

## Reading the history

- Expose the history through a typed `AuditLogResponse` built by an `AuditLogResponse(AuditLog log)` constructor. Never a `Map`.
- Listing endpoints take an `AuditLogFilter` (`entityType`, `entityId`, `userId`, `action`, `startDate`, `endDate`) plus `Pageable`, and return `Page<AuditLogResponse>` — per `spring-backend-standards` and `spring-filters-specification`.
- Every audit read is `@Transactional(readOnly = true)`.

## Never log secrets

Strip sensitive fields from `oldValue` / `newValue` before saving. Maintain the denylist in one constant next to the serializer:

```
password, senha, passwordHash, passwordEnc, token, tokenEnc, secret, apiKey, apiKeyValueEnc
```

Truncate serialized values to the column length (leave a margin, e.g. 1990 of 2000) rather than letting the insert blow up mid-transaction.

## Constraints

- Do not introduce Hibernate Envers (`@Audited`, `spring-data-envers`). No backend here uses it; every trail is explicit and stays that way.
- Do not create a second audit table in a backend that already has one — extend the existing one.
- Do not add setters to `AuditLog`, and never `UPDATE` or `DELETE` a row. Retention is a purge job, logged as its own action.
- Do not audit reads by default. Log `QUERY`/`EXPORT` only when the data is sensitive enough to justify the volume.
- Do not put audit calls in controllers, entity callbacks, or an AOP aspect that guesses the action from the method name — the service knows what happened; a proxy only knows what was invoked.
