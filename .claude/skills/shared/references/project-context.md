# Project Context

Frontend application built with **React + TypeScript** using a **feature-oriented architecture**.

Most domain logic lives inside `features`.

---

# Project Structure

src/

api/  
API consumption logic (axios client, DTOs, TanStack Query hooks)

app/  
application bootstrap (providers, router, App.tsx)

components/  
reusable UI components shared across features

features/  
feature modules containing pages, components and feature-specific logic

hooks/  
reusable global hooks

lib/  
shared business logic utilities

utils/  
pure utility functions

---

# Feature Structure

Typical feature layout:

features/

module-name/
feature-name/
api/
components/
hooks/
pages/
index.ts

Feature logic should remain inside the feature whenever possible.  
Only move code to global folders when it is reused across multiple features.

See the `feature-structure` skill for complete structure rules, complexity levels, and index export conventions.

---

# Coding Principles

General guidelines:

- Use **React functional components**
- Use **TypeScript**
- Prefer **named exports**
- Prefer **composition over large components**
- Extract complex logic into hooks

Avoid:

- large components
- duplicated logic
- mixing UI and business logic

---

# TypeScript Rules

Strict typing is required.

Avoid unless absolutely necessary:

- `any`
- `@ts-ignore`
- forced type assertions (`as SomeType`)

Prefer:

- proper type definitions
- generics
- safe type guards

Never bypass TypeScript errors without explaining why.

---

# Code Quality

Generated code must:

- be clean and readable
- avoid unnecessary abstractions
- avoid overly clever implementations
- follow existing project patterns

Comments should not exist except for **JSDoc annotations**, and must be written in **Brazilian Portuguese**.
When the code is clean, comments should not be necessary.

---

# Data Fetching

The project uses **TanStack Query**.

Rules:

- API clients and DTOs live in `src/api`
- Queries and mutations should follow consistent patterns
- Prefer wrapping queries inside hooks instead of using them directly in components

---

# Refactoring Guidelines

When refactoring:

- preserve existing behavior
- improve separation of concerns
- reduce component complexity
- extract reusable hooks when appropriate
- maintain feature boundaries

---

# Naming Conventions

Files:

- Components: `PascalCase.tsx` — e.g., `StatusBadge.tsx`, `UserClientsModal.tsx`
- Pages: `PascalCasePage.tsx` — e.g., `UsersPage.tsx`, `UserFormPage.tsx`
- Hooks: `useXxx.ts` — e.g., `useUserFormSubmit.ts`, `useDashboardData.ts`
- Service hooks: `useXxxService.ts` — e.g., `useUserService.ts`
- Utilities: `camelCase.ts` — e.g., `completionRate.ts`, `formatDuration.ts`
- API DTO files: `camelCase.ts` — e.g., `user.ts`, `role.ts`

Types:

- API DTO types: `XxxApiDto` — e.g., `UserApiDto`, `RoleApiDto`
- Request types: `CreateXxxRequest`, `UpdateXxxRequest`
- Form value types: `XxxFormValues` — e.g., `UserFormValues`
- Hook param types: `UseXxxParams` — e.g., `UseUserFormSubmitParams`

---

# Import Paths

`@/` maps to `src/`. Use it for all cross-feature and cross-folder imports:

```ts
import { Button } from '@/components/button/Button';
import { usePagination } from '@/hooks/usePagination';
import type { UserApiDto } from '@/features/accessControl/user/api';
```

Use relative imports only within the same feature.

---

# UI Patterns

Before creating a new UI component, check `src/components/` — it may already exist.

When presenting a form or action, apply this decision:

| Pattern    | When to use                                                              |
| ---------- | ------------------------------------------------------------------------ |
| **Sheet**  | Create/edit forms with simple to moderate fields                         |
| **Page**   | Create/edit forms with many fields, nested data, or complex interactions |
| **Dialog** | Confirmations and small auxiliary actions — not for CRUD forms           |

---

# AI Collaboration Workflow

When solving problems:

1. Analyze the relevant files
2. Explain the reasoning
3. Propose a plan
4. Implement the solution
