# Security Patterns

## Common building blocks

- Security configuration
- Authentication filter
- Token service
- Authenticated-user helper or service

## Behavior

- `csrf` disabled
- `cors` enabled
- session policy: `STATELESS`
- docs routes permitted
- every other route authenticated

## Token flow

1. Read cookie `token`
2. Fallback to `Authorization` bearer header
3. Validate signature and issuer in a dedicated token service
4. Extract subject
5. Build `UsernamePasswordAuthenticationToken`
6. Save auth in `SecurityContextHolder`

## Logged-user usage

- A user helper often converts the authenticated principal to an application user id
- Services and DTOs may call an integration service when they need user details from another system
