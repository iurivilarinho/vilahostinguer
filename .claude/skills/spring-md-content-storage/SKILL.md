---
name: spring-md-content-storage
description: Store Markdown or other long textual file content in Java Spring entities using JPA `@Lob` with `String`. Use when Codex needs to create or update an entity field that persists `.md` content or other large text content directly in the database.
---

# Spring MD Content Storage

Use this skill when the domain stores Markdown text or another large textual payload inside the entity itself.

## Rules

- Do not use Lombok.
- Keep field and method names in English.
- Use `snake_case` in `@Column(name = ...)` when a custom column name is needed.
- Use the field below for Markdown content persisted in the database.

```java
@Lob
private String content;
```

## Notes

- Use `String` with `@Lob` for textual content such as `.md` files.
- Keep binary files out of this pattern. For images, videos, PDFs, or arbitrary file bytes, use `spring-document-storage`.
