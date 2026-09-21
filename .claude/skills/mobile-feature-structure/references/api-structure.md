# Feature API Structure

Each feature that talks to a backend can have its own `api/` folder. In this offline-first app, `api/` is optional and should stay simpler than a web app data layer. There is no TanStack Query here.

Realm should remain the local source of truth for persisted feature data. The `api/` layer is responsible only for remote contracts and request functions.

---

## Folder layout

```text
feature/
  api/
    dtos/
      feature.ts        ← TypeScript interfaces for this feature's API contracts
    services/
      featureService.ts     ← plain async functions for backend calls
    index.ts            ← re-exports
```

---

## dtos/

Contains TypeScript interfaces that match the API request and response shapes used by this feature.

Feature-level DTOs are kept inside the feature. Move to `src/api/dtos/` only when the same DTO is consumed by multiple features.

```typescript
// user/api/dtos/user.ts
export interface UserDto {
  id: string;
  name: string;
  email: string;
}
```

---

## services/

Contains plain async functions that perform backend calls. Do not create query hooks here.

```typescript
// user/api/services/userApi.ts
export async function getUsers() {
  // call HTTP client here
}

export async function createUser(payload: CreateUserRequest) {
  // call HTTP client here
}
```

The feature should map API payloads into Realm models through `realm/adapters/` before using them in screens or forms.

---

## api/index.ts

Exports only API services. Never re-exports DTOs.

```typescript
// user/api/index.ts
export * from './services/userService';
```

```typescript
// checklist/api/index.ts
export * from './services/checklistService';
export * from './services/workTasksService';
```

---

## Rules

- feature DTOs live inside the feature, not in `src/api/dtos/`
- `api/index.ts` exports services only — DTOs are internal to the feature
- external code imports services through the feature root `index.ts`, never directly from `feature/api/services/`
- do not treat `api/` as the source of truth for persisted data in an offline-first feature
