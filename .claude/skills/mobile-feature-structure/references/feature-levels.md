# Feature Complexity Levels

Features grow through complexity tiers. Always start at the simplest tier that fits and grow only when needed.

---

## Minimal

Screens only. No domain API, no Realm persistence, no reusable feature components.

Use for static flows, informational screens, or very small features with no persisted domain data.

**Examples**: `about`, `terms`, `permissions`

```text
src/features/accessControl/permissions/
  screens/
    PermissionsScreen.tsx
  index.ts
```

```typescript
// permissions/index.ts
export * from './screens/PermissionsScreen';
```

---

## Standard

Screens + components + realm. The most common offline-first feature shape.

Use when the feature persists domain data locally and has at least one reusable UI component.

**Examples**: `profile`, `task`, `inventory`

```text
src/features/operational/task/
  components/
    TaskForm.tsx
  realm/
    adapters/
      mapTaskRealmToForm.ts
      mapTaskApiToRealm.ts
    schemas/
      TaskSchema.ts
    services/
      getTaskById.ts
      upsertTask.ts
  screens/
    TasksScreen.tsx
  index.ts
```

```typescript
// task/index.ts
export * from './screens/TasksScreen';
```

---

## Enhanced

Standard + `hooks/` (+ optional `api/`). Add `hooks/` when the feature has complex cross-screen or cross-component logic.

Use when a screen or component accumulates logic that is hard to follow or test in isolation. Add `api/` only if the feature syncs with a backend.

**Examples**: `user`, `checklist`

```text
src/features/accessControl/user/
  api/
    dtos/
      user.ts
    services/
      userApi.ts
    index.ts
  components/
    UserClientsModal.tsx
    UserProfilesModal.tsx
  hooks/
    useUserFormSubmit.ts
  realm/
    adapters/
      mapUserApiToRealm.ts
      mapUserRealmToForm.ts
    schemas/
      UserSchema.ts
    services/
      getUserById.ts
      upsertUser.ts
  screens/
    UsersScreen.tsx
    UserFormScreen.tsx
  index.ts
```

```typescript
// user/index.ts
export * from './screens/UsersScreen';
export * from './screens/UserFormScreen';
export * from './api';
```

---

## Comprehensive

Enhanced + `utils/` (+ optional `mock/`). Reserved for large, high-complexity features.

Add `utils/` only for pure transformation functions that belong to this domain but are not hooks or Realm adapters.
Add `mock/` only during active development when mock data is needed.

**Examples**: `dashboard`, `inspection`

```text
src/features/dashboard/
  api/
    dtos/
      checklistPerformance.ts
    services/
      dashboardApi.ts
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
  realm/
    adapters/
      mapDashboardApiToRealm.ts
      mapDashboardRealmToCard.ts
    schemas/
      DashboardSnapshotSchema.ts
    services/
      getLatestDashboardSnapshot.ts
      upsertDashboardSnapshot.ts
  mock/
  screens/
    DashboardScreen.tsx
  utils/
    completionRate.ts
    formatDuration.ts
  index.ts
```

```typescript
// dashboard/index.ts
export * from './screens/DashboardScreen';
export * from './api';
```

---

## Decision guide

Start with **Minimal**. Add `components/` + `realm/` → **Standard**. Add `hooks/` → **Enhanced**. Add `utils/` → **Comprehensive**. Add `api/` only when remote sync or backend integration is required.

Only create a folder when there is real content to put in it.
