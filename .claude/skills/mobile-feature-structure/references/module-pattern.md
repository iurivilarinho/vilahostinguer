# Module Pattern

Related features are grouped under a **module folder** instead of being placed directly at the top level of `src/features/`.

---

## Current modules

```text
src/features/
  accessControl/       ← module folder
    auth/
    jobPosition/
    permissions/
    role/
    user/
  operational/         ← module folder
    asset/
    checklist/
    client/
    environment/
  dashboard/           ← standalone feature (no module)
  shared/              ← cross-feature utility screens
    notFound/
```

---

## Module folder rules

- A module folder is **not a feature**. It has no `screens/`, `components/`, `api/`, or `realm/` of its own.
- Each subfolder inside a module is a fully self-contained feature.
- Module folders do not have a root `index.ts` that re-exports all subfeatures.

---

## When to create a module

Create a module folder when two or more features share a domain concept.

Examples:

- `accessControl/` groups auth, roles, users, permissions — all part of access management
- `operational/` groups checklists, assets, clients, environments — all part of day-to-day operations

Do not create a module for a single feature. Use a standalone feature instead.

---

## shared/

`shared/` holds cross-feature utility screens that do not belong to any domain module.

```text
src/features/shared/
  notFound/
    screens/
      NotFoundScreen.tsx
    index.ts
```

Do not put domain-specific components, hooks, or realm code inside `shared/`. Those belong inside their own feature.

---

## Standalone features

Features that do not belong to any module are placed directly under `src/features/`:

- `dashboard/` — a standalone comprehensive feature

Use a standalone top-level feature when it does not share a clear domain with other features and is unlikely to grow into a group.
