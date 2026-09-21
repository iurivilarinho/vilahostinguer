---
name: spring-security-jwt
description: Implement or adjust authentication and authorization in Java Spring backends using a stateless Spring Security plus JWT pattern. Use when Codex needs `SecurityFilterChain`, custom `OncePerRequestFilter`, JWT validation, cookie or Bearer token extraction, authenticated user resolution, protected endpoints, or documentation-route exceptions.
---

# Spring Security JWT

Follow the target backend's current authentication model instead of introducing a new one.

## Read first

- The current security configuration
- The current authentication filter, if present
- The token utility or token service
- The service used to resolve the authenticated user
- `references/security-patterns.md`

## Security model

- The application is stateless.
- JWT can arrive by cookie named `token` or by `Authorization: Bearer ...`.
- A custom authentication filter usually validates the token before the username/password filter.
- The authenticated principal is the subject string from the token, usually a user id.
- Documentation endpoints are often publicly accessible.

## Implementation rules

- Keep request authentication in the custom filter.
- Put token parsing and verification logic in a dedicated token service or utility.
- When a token is invalid or missing, return JSON with timestamp and message.
- Resolve the logged user id from `SecurityContextHolder` in a dedicated auth-aware service or helper.
- Keep external user profile lookups outside the filter.

## Constraints

- Do not switch this project to session-based auth.
- Do not introduce a separate user-details persistence layer unless explicitly requested.
- Preserve compatibility with both cookie and bearer token transport.
- Document secured endpoints and auth-related request/response contracts with Swagger/OpenAPI when the API is documented.
