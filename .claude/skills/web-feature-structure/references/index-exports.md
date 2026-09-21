# Index Export Conventions

Every feature has a root `index.ts` that controls what is publicly accessible from outside the feature.
Features with an `api/` folder also have an `api/index.ts` as a sub-index.

---

## Feature root index.ts patterns

### Pages only

Use when the feature has no public API services — typically Minimal features.

```typescript
// auth/index.ts
export * from "./pages/LoginPage";
```

### Pages + api

Use when the feature has an `api/` folder with service hooks.

```typescript
// user/index.ts
export * from "./pages/UsersPage";
export * from "./pages/UserFormPage";
export * from "./api";
```

```typescript
// checklist/index.ts
export * from "./pages/ChecklistsPage";
export * from "./pages/ChecklistFormPage";
export * from "./api";
```

### Single page, no public api

Use for standalone features with one route-level page and no services to export externally.

```typescript
// dashboard/index.ts
export * from "./pages/DashboardPage";
```

---

## api/index.ts pattern

Always exports only service hooks. Never exports DTOs.

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

- Always export pages from the feature root `index.ts`
- Export `./api` from the feature root only when the feature has an `api/` folder
- Never export DTOs from `api/index.ts` — they are internal to the feature
- Never import directly from `feature/api/services/Foo` — always go through the feature root `index.ts`
