# Error and Logging Patterns

## Error handler

- Format: `{ timestamp, message: [] }`
- Covered cases:
  - `EntityNotFoundException` -> `404`
  - `DataIntegrityViolationException` -> `409`
  - `AccessDeniedException` -> often `401` or `403`, depending on the backend
  - `IllegalArgumentException` -> `400`
  - `MethodArgumentNotValidException` -> `400`

## Logging interceptor

- Generates request id
- Adds `X-Request-Id` header
- Logs method, URI, params, user-agent, status, duration, and end timestamp

## Registration

- Register the interceptor through the Spring MVC config class

## Usage note

- Keep logging implementation in infrastructure classes, not in business services.
