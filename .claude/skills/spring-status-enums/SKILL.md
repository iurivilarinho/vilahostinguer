---
name: spring-status-enums
description: Create or update status enums with transition rules in Java Spring backends. Use when Codex needs centralized status definitions, nested enum organization inside a shared types file, transition validation, Portuguese descriptions, Swagger-documented enum values, or service rules that must validate allowed status changes before persisting.
---

# Spring Status Enums

Use a centralized status-enum pattern for workflows that have explicit transition rules.

## Read first

- The shared types or enums file used by the target codebase
- Any existing status enum with transition rules
- The entity and service that persist and update the status
- `references/status-patterns.md`

## Structure

- Prefer keeping status enums inside a shared `Types.java` file when the target codebase uses that convention.
- For workflow status enums, keep:
  - enum constants with Portuguese descriptions
  - a `description` field
  - a static immutable transition map
  - `canTransitionTo(...)`
  - `validateTransition(current, target)`

## Rules

- Document the enum and each constant with Swagger/OpenAPI annotations.
- Store user-facing descriptions in Portuguese unless the codebase uses another language.
- Use `EnumMap` for transition configuration.
- Expose transition validation at enum level when the business rule is intrinsic to the status lifecycle.
- Throw `IllegalStateException` with a clear message when a transition is invalid.
- Keep terminal statuses with `Collections.emptySet()`.

## Service usage

- Before persisting a new status, validate `current -> target`.
- Keep persistence code in the service layer; keep transition rules in the enum.
- When the entity stores the enum with JPA, prefer `@Enumerated(EnumType.STRING)`.

## Constraints

- Do not scatter transition rules across multiple services.
- Do not duplicate the same transition matrix in controllers.
- If the project already has a shared `Types` file, extend it instead of creating isolated enum files.
