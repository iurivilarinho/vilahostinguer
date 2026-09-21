# Audit Log Patterns

## What exists today

Every backend hand-rolls its own trail. There is no shared library and **no Hibernate Envers anywhere** — do not assume a base class or starter exists.

| Backend | Class | Actor column | Transaction |
| --- | --- | --- | --- |
| cronos | `audit/AuditLog` | `Long userId` | joins caller (`@Transactional`) |
| Integrador | `audit/AuditLog` | `String performedBy` | `REQUIRES_NEW` + swallow |
| rifas | `models/AuditLog` | `Long userId` | swallows via try/catch |
| comandas | `models/AuditLog` | `String actorName` (caller-supplied) | joins caller |
| arranhacel, casa-smart | `models/AuditLog` | `String actorName` | joins caller |

`cronos` is the closest to the target shape: enum action, before/after, reason, source, three indexes, append-only by construction. Read it before writing a new one.

Movement history with quantity already exists in WMS:

- `wms/movement/models/MovementHistory` — `tbHistoricoMovimentacoes`, `@ManyToOne User`, `BigDecimal quantity`, `TipoMovimentacao type`
- `wms/logs/models/PalletActionLog` — documented as "trilha de auditoria"; carries `previousStatus` as the before-value

## Known defects in the existing copies

Do not propagate these when copying from a neighbour:

- `back/backend-governanca/models/Movimentacoes` — `tipo` has **no `@Enumerated(EnumType.STRING)`**, so it persists as an ordinal. It also sets `data` in the constructor instead of `@PrePersist`.
- Integrador's `AuditAction` is a `public final class` of String constants, not an enum (deliberate, to avoid migrations). New code uses an enum; leave Integrador's alone.
- The actor type is genuinely inconsistent (`Long` vs `String`). New code uses `Long userId` + a `userName` snapshot.

## Resolving the logged user

Two competing patterns; **no shared class**. Match whichever the target backend already has:

**A — dedicated `AuthenticatedUserService`** (Integrador, porteiro, posto, constructorHub):

```java
public User getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
        throw new EntityNotFoundException("Nenhum usuário autenticado no contexto.");
    }
    return user;
}
public Long getAuthenticatedUserId() { return getAuthenticatedUser().getId(); }
```

**B — inline in `UserService`** (checklist, comandas, arranhacel, casa-smart, governanca, cronos, rifas, entregas, painel-fiscal):

```java
public User findUserLoggedIn() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) return null;
    Object principal = authentication.getPrincipal();
    if (principal instanceof User user) return userRepository.findById(user.getId()).orElse(null);
    return userRepository.findByLogin(authentication.getName()).orElse(null);
}
```

Two outliers to watch for: **Integrador**'s principal *is* the raw id (`return (Long) auth.getPrincipal();`), and **painel-fiscal**'s JWT subject is the numeric id (`Long.parseLong(authentication.getName())`), which throws on a non-numeric name.

## No logged user

A background job, scheduler, import, or integration has no security context. Decide explicitly rather than letting a `null` slip into `user_id`:

- Leave `userId` null and set `userName` to a constant (`"system"`, `"integracao"`) — the shape existing backends already degrade to.
- Never throw from the audit path just because there is no user; the action still happened.

## Sensitive fields

Denylist observed in Integrador, worth reusing verbatim:

```
password, senha, passwordEnc, tokenEnc, apiKeyValueEnc, sessionRequestBody, token, secret, passwordHash
```

## Serialization notes

- `oldValue`/`newValue` are JSON strings. SQL Server backends that need more than 2000 chars use `NVARCHAR(MAX)` (Integrador does); otherwise truncate with a margin (rifas truncates at 1990).
- Serialize a small snapshot type, not the JPA entity — serializing the entity drags lazy associations and can trigger `LazyInitializationException` or dump an entire object graph into the column.
