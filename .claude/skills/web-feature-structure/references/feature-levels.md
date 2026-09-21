# Feature Complexity Levels

Features grow through complexity tiers. Always start at the simplest tier that fits and grow only when needed.

---

## Minimal

Pages only. No domain API, no shared components.

Use for UI-only pages or auth flows without complex data-fetching logic.

**Examples**: `auth`, `permissions`

```text
src/features/accessControl/auth/
  pages/
    LoginPage.tsx
  index.ts
```

```typescript
// auth/index.ts
export * from "./pages/LoginPage";
```

---

## Standard

Pages + components + api. The most common feature shape.

Use when the feature has its own data fetching and at least one reusable UI component.

**Examples**: `role`, `client`, `environment`

```text
src/features/accessControl/role/
  api/
    dtos/
      role.ts
    services/
      useRoleService.ts
    index.ts
  components/
    RoleForm.tsx
  pages/
    RolesPage.tsx
  index.ts
```

```typescript
// role/index.ts
export * from "./pages/RolesPage";
export * from "./api";
```

---

## Enhanced

Standard + `hooks/`. Add `hooks/` when the feature has complex cross-component logic that doesn't belong inline.

Use when a page or component accumulates logic that's hard to follow or test in isolation.

**Examples**: `user`, `checklist`

```text
src/features/accessControl/user/
  api/
    dtos/
      user.ts
    services/
      useUserService.ts
    index.ts
  components/
    UserClientsModal.tsx
    UserProfilesModal.tsx
  hooks/
    useUserFormSubmit.ts
  pages/
    UsersPage.tsx
    UserFormPage.tsx
  index.ts
```

```typescript
// user/index.ts
export * from "./pages/UsersPage";
export * from "./pages/UserFormPage";
export * from "./api";
```

---

## Comprehensive

Enhanced + `utils/` (+ optional `mock/`). Reserved for large, high-complexity features.

Add `utils/` only for pure transformation functions that belong to this domain but are not hooks.
Add `mock/` only during active development when mock data is needed.

**Examples**: `dashboard`

```text
src/features/dashboard/
  api/
    dtos/
      checklistPerformance.ts
    services/
      useChecklistPerformance.ts
    index.ts
  components/
    DashboardHeader.tsx
    KpiStrip.tsx
    InsightsStrip.tsx
    charts/
  hooks/
    useDashboardData.ts
    useDashboardFilter.ts
    useDashboardInsights.ts
  mock/
  pages/
    DashboardPage.tsx
  utils/
    completionRate.ts
    formatDuration.ts
  index.ts
```

```typescript
// dashboard/index.ts
export * from "./pages/DashboardPage";
```

---

## Decision guide

Start with **Minimal**. Add `api/` + `components/` → **Standard**. Add `hooks/` → **Enhanced**. Add `utils/` → **Comprehensive**.

Only create a folder when there is real content to put in it.
