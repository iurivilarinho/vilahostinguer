# Status Patterns

## Recommended enum shape

- Swagger `@Schema` on the enum and constants
- Portuguese description field
- Immutable transition map
- `canTransitionTo(target)`
- `validateTransition(current, target)`

## Transition design

- Use `EnumMap<StatusType, Set<StatusType>>`
- Build the transition map in a static block
- Wrap with `Collections.unmodifiableMap(...)`
- Use `Collections.emptySet()` for terminal statuses

## Integration notes

- Services validate transitions before saving
- Entities usually persist the enum as `EnumType.STRING`
- Responses can expose both enum name and description when needed
