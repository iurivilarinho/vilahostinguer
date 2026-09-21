---
name: spring-error-logging
description: Standardize API error responses and HTTP request logging in Java Spring backends using a global handler and interceptor approach. Use when Codex needs `@RestControllerAdvice`, structured validation errors, exception-to-status mapping, request and response timing logs, request ids, or Spring MVC interceptor registration.
---

# Spring Error Logging

Reuse the target backend's global error and request logging pattern.

## Read first

- The current global error handler
- The current logging interceptor, if present
- The Spring MVC config that registers interceptors
- Any success or response wrapper already used by the project
- `references/error-logging-patterns.md`

## Error handling rules

- Centralize API exceptions in `@RestControllerAdvice`.
- Return a payload with `timestamp` and a list of `message` strings.
- Map common framework and domain exceptions explicitly.
- Collect Jakarta validation errors from `MethodArgumentNotValidException`.
- Prefer propagating exceptions from services instead of formatting them inline in controllers.
- Document error responses in Swagger/OpenAPI when the API uses generated docs.

## Logging rules

- Use `HandlerInterceptor` for request lifecycle logs.
- Attach a request id to both request attributes and response headers.
- Log request start, request end, status code, and duration.
- Keep logging registration in the Spring MVC configuration class.

## Constraints

- Do not spread ad-hoc `try/catch` blocks across controllers just to format errors.
- Keep the JSON error shape compatible with the current handler unless the task explicitly asks for a breaking change.
