# Index Export Conventions

Every feature has a root `index.ts` that controls what is publicly accessible from outside the feature.
Features with an `api/` folder also have an `api/index.ts` as a sub-index.
`realm/` stays internal by default and should not be re-exported from the feature root unless there is a deliberate and reviewed reason.

---

## Feature root index.ts patterns

### Screens only

Use when the feature has no public API services — typically Minimal features.

```typescript
// auth/index.ts
export * from './screens/LoginScreen';
```

### Screens + api

Use when the feature has an `api/` folder with simple request services.

```typescript
// user/index.ts
export * from './screens/UsersScreen';
export * from './screens/UserFormScreen';
export * from './api';
```

```typescript
// checklist/index.ts
export * from './screens/ChecklistsScreen';
export * from './screens/ChecklistFormScreen';
export * from './api';
```

### Single screen, no public api

Use for standalone features with one route-level screen and no services to export externally.

```typescript
// dashboard/index.ts
export * from './screens/DashboardScreen';
```

---

## api/index.ts pattern

Always export only API services. Never export DTOs.

```typescript
// user/api/index.ts
export * from './services/userApi';
```

```typescript
// checklist/api/index.ts
export * from './services/checklistApi';
export * from './services/workTasksApi';
```

---

## Rules

- Always export screens from the feature root `index.ts`
- Export `./api` from the feature root only when the feature has an `api/` folder
- Never export DTOs from `api/index.ts` — DTOs are internal to the feature
- Never export `realm/` from the feature root by default
- Never import directly from `feature/api/services/Foo` — always go through the feature root `index.ts`
