---
name: web-api-integration
description: Implements React frontend API integrations using TanStack Query with a standardized architecture based on custom hooks, query patterns, and mutation patterns. Use when creating or modifying API calls, queries, or mutations in React applications.
---

# API Integration Skill

This project integrates backend APIs using **TanStack Query wrapped in custom hooks**.

When implementing or modifying API integrations, follow the architecture and rules defined in the reference documents.

## Architecture Overview

All API communication follows this structure:

1. **API functions**
   - Responsible only for performing HTTP requests.
   - Located in `src/api/clients`.

2. **React Query hooks**
   - Wrap API functions.
   - Provide caching, invalidation, and configuration.
   - Located inside the feature: `features/<name>/api/services/useXxxService.ts`
   - Only hooks reused across multiple features go in `src/hooks`

3. **Components**
   - Must **only use hooks**
   - Must **never call `apiClient` directly**

This separation ensures:

- consistent caching
- reusable hooks
- predictable query keys
- UI decoupling from API implementation

---

# Shared Types

See the type definitions and import paths:

- `references/types.md`

Covers:

- `QueryOptions<T>` — extends TanStack's options, always omits `queryKey`
- `MutationOptions<TData, TVariables>` — custom interface (not TanStack's), controls toast & callbacks
- `ApiRequestParams<T, TFilter>` — standard paginated request envelope
- `ApiPaginatedResponse<T>` — Spring Boot Page shape, 0-based page numbering
- `ApiSortParam<T>` — type-safe sort descriptor

---

# Implementation Rules

See the detailed rules:

- `references/rules.md`

These rules define:

- architectural constraints
- folder structure
- query key guidelines
- hook responsibilities

---

# Query Patterns

For standard query implementations see:

- `references/query-patterns.md`

Includes:

- basic queries
- parameterized queries
- paginated queries with `ApiRequestParams` and `usePagination`
- `enabled` usage
- query key rules

---

# Mutation Patterns

For mutation implementations see:

- `references/mutation-patterns.md`

Includes:

- mutation structure
- query invalidation
- `resolveErrorMessage` for error toasts
- `successMessage` as string or function
- `showToast` opt-out pattern

---

# Query Key Conventions

For query key structure and rules see:

- `references/query-keys.md`

Includes:

- non-paginated key patterns
- paginated key patterns with destructured params
- invalidation-friendly key design

---

# Usage Guidance

Use this skill when:

- Implementing a **new API integration**
- Creating **React Query hooks**
- Adding **queries or mutations**
- Implementing **paginated data fetching**
- Refactoring existing API calls to match the project architecture
- Ensuring **consistent TanStack Query usage**

Always follow the reference patterns before introducing custom variations.
