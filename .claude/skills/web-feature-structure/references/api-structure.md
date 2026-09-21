# Feature API Structure

Each feature that fetches data has its own `api/` folder. This is separate from the global `src/api/`, which holds only the axios client and shared infrastructure DTOs.

---

## Folder layout

```text
feature/
  api/
    dtos/
      feature.ts        ← TypeScript interfaces for this feature's API contracts
    services/
      useFeatureService.ts  ← TanStack Query hooks for this feature
    index.ts            ← re-exports services only
```

---

## dtos/

Contains TypeScript interfaces that match the API response shapes used by this feature.

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

Contains custom hooks that wrap TanStack Query. Each service file is named `useFooService.ts`.

See the `api-integration` skill for full patterns on how to write these hooks.

---

## api/index.ts

Exports only service hooks. Never re-exports DTOs.

```typescript
// user/api/index.ts
export * from "./services/useUserService";
```

```typescript
// checklist/api/index.ts
export * from "./services/useChecklistService";
export * from "./services/useWorkTasksService";
```

---

## Rules

- Feature DTOs live inside the feature, not in `src/api/dtos/`
- `api/index.ts` exports services only — DTOs are internal to the feature
- External code imports services through the feature root `index.ts`, never directly from `feature/api/services/`
