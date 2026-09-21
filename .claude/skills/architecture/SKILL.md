---
name: architecture
description: Project architecture rules for this React Vite web app. Use when planning, implementing, reviewing, or refactoring features, pages, API modules, state, folder structure, imports, or migrations away from legacy src/api into feature-local API folders.
---

# Architecture

Use vertical feature slices. Domain UI, API, types, hooks, validation, and local helpers belong in `src/features/<feature>`.

`src/api` must not exist in this project. Domain API code belongs in the owning feature; shared API infrastructure belongs in `src/lib/api`.

## Feature Shape

```txt
src/features/<feature>/
  api/
    index.ts
    keys.ts
    service.ts
    types.ts
  model/
    index.ts
    use-<feature>-store.ts
  <screen>/
    page-<screen>.tsx
    form/
    components/
    utils/
```

Create only folders the feature actually uses. Small features may have `page-<feature>.tsx` at the feature root.

## Global Infrastructure

Keep shared, non-domain infrastructure outside features:

- `src/app`: providers, routing, global hooks, route constants, app-wide utilities.
- `src/components`: reusable UI primitives and composed app modules. Each reusable component must live in its own folder with `index.ts` and a colocated `.stories.tsx`.
- `src/lib`: generic non-domain helpers.

Preferred API target:

```txt
src/lib/api/
  clients/
  types/
  utils/
```

Do not recreate `src/api`, `src/api/services`, or `src/api/types`.

## Imports

- Import feature API from `@/features/<feature>/api`.
- Import feature state from `@/features/<feature>/model`.
- Import UI and shared composed components from the public barrel `@/components`.
- Use `@/components/<group>/...` only for component internals or when a grouped file is intentionally not exported by the barrel.
- Import shared app infrastructure from `@/app/...`.
- Never import from `@/api/...`.

```ts
import { useCustomerListQuery } from "@/features/customer/api";
import { Button } from "@/components";
```

## Naming

- Use kebab-case for new folders and files.
- Use `page-<screen>.tsx` for screen/page entry components when creating or migrating pages.
- Use `service.ts`, `types.ts`, `keys.ts` inside feature `api/`.
- Use `index.ts` only as a narrow public export for submodules such as `api/` and `model/`.
- Keep `src/components/index.ts` as the public export surface for reusable components.
- Keep reusable component examples documented in `src/features/security/design-system/page-design-system.tsx`.

Existing legacy names may remain until touched.

## Code Conventions

- Prefer arrow functions and named exports.
- Use `type` instead of `interface` for new types.
- Use `import type` for type-only imports.
- Avoid direct Axios calls in pages/components; wrap them in feature API services/hooks.
- Avoid magic numbers in shared logic; extract named constants.

## Migration Checklist

- Create `src/features/<feature>/api`.
- Move domain DTOs/types into `api/types.ts`.
- Move HTTP functions and TanStack Query hooks into `api/service.ts`.
- Move query keys into `api/keys.ts`.
- Export through `api/index.ts`.
- Update imports to `@/features/<feature>/api`.
- Delete any accidental `src/api` files when no imports remain.
- Run build/lint after each domain migration.
