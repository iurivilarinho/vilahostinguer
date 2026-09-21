---
name: web-feature-structure
description: guide the structure of a new react web feature. use when asked to create, organize, or review files inside src/features, decide where pages, components, hooks, or api files should live, and determine whether something should remain inside the feature or move to shared src folders.
---

Use this skill to propose a structure for a new React web feature.

Keep the feature small and cohesive.
Do not over-engineer.
Do not create folders that are not needed.

## Folder roles

- `pages/`: route-level pages and page-specific components
- `components/`: UI components used only by this feature
- `hooks/`: feature-specific logic hooks
- `api/`: DTOs and service hooks for this feature's API calls (see [api-structure](./references/api-structure.md))
- `utils/`: pure helper functions used only by this feature
- `mock/`: mock data or fixtures (optional — only when needed during development)

## Core rules

- keep code inside the feature by default
- move to shared `src/*` only after real reuse across multiple features
- if something has domain language in its name, it probably stays inside the feature
- a feature may have one or many pages
- do not split into multiple features only because one page navigates to another
- related features should be grouped under a module folder (see [module-pattern](./references/module-pattern.md))

## Feature complexity levels

Choose the right level for the feature's needs. See [feature-levels](./references/feature-levels.md) for full examples.

| Level         | Folders                           | Examples                  |
| ------------- | --------------------------------- | ------------------------- |
| Minimal       | `pages/`                          | auth, permissions         |
| Standard      | `pages/` + `components/` + `api/` | role, client, environment |
| Enhanced      | Standard + `hooks/`               | user, checklist           |
| Comprehensive | Enhanced + `utils/`               | dashboard                 |

Do not start at Comprehensive. Start at the appropriate level and grow only when needed.

## Quick decision rules

### Keep inside the feature

Keep it inside the feature when:

- it is used only there
- it has domain-specific naming
- it depends on feature-specific logic

Examples:

- `useOrderForm`
- `OrderSummary`
- `OrderFilters`

### Move to shared `src/*`

Move it only when:

- it is reused across multiple features
- it is generic
- it no longer depends on one domain

Examples:

- `Button`
- `EmptyState`
- `PageHeader`
- `useDebouncedValue`

## Smells

These are signs the structure is getting bad:

- giant page files
- pages calling API directly
- components with business logic
- generic shared extraction too early

## References

- [feature-levels](./references/feature-levels.md) — folder trees for each complexity tier
- [api-structure](./references/api-structure.md) — layout of the `api/` subfolder inside features
- [module-pattern](./references/module-pattern.md) — how module folders group related features
- [index-exports](./references/index-exports.md) — index.ts export conventions
- too many empty folders
- unrelated domains mixed in one feature

## Expected output

When using this skill:

1. propose the smallest valid feature tree
2. explain which folders are needed
3. do not add unnecessary folders
4. keep feature-specific code local
5. call out anything that should move to shared `src/*` only if reuse is clear
6. export pages from `index.ts` for easy routing imports
