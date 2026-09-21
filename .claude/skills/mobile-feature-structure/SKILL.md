---
name: mobile-feature-structure
description: guide the structure of a new react native feature. use when asked to create, organize, or review files inside src/features, decide where screens, components, hooks, api, or realm files should live, determine whether something should remain inside the feature or move to shared src folders, and flag inconsistencies for offline-first apps that use realm as the local source of truth.
---

This app is offline-first. Realm is the local source of truth for persisted data. The API is a sync boundary, not a runtime data source.

The default rule is: **keep code inside the feature**. Moving something to shared `src/*` requires real reuse across multiple features — not just a feeling it might be useful later. Premature extraction creates coupling without benefit.

## Workflow

1. Identify what's being asked:
   - **New feature** → choose a complexity tier, propose the folder tree
   - **File placement** → apply the keep-vs-move decision below
   - **Review** → run the offline-first consistency checks
2. Propose only folders that have real content — empty folders are noise
3. Flag any inconsistencies found and suggest the smallest fix

## Folder roles

| Folder | Purpose | Notes |
|--------|---------|-------|
| `screens/` | Route-level screens | Always `screens/`, never `pages/` |
| `components/` | UI components local to this feature | |
| `hooks/` | Logic hooks with state or side effects | Only when logic outgrows a screen |
| `realm/` | Local persistence: `schemas/`, `services/`, `adapters/` | Required when feature persists domain data |
| `api/` | Remote contracts + request functions | Optional; never the runtime source of truth |
| `utils/` | Pure helper functions, no side effects | Only for Comprehensive features |
| `mock/` | Dev-only fixtures | Delete before shipping |

## Complexity tiers — start low, grow only when needed

| Tier | Folders | Use when |
|------|---------|----------|
| Minimal | `screens/` | Static screens, no persisted domain data |
| Standard | `screens/` + `components/` + `realm/` | Feature persists domain data locally |
| Enhanced | Standard + `hooks/` (+ optional `api/`) | Logic is complex or feature syncs with backend |
| Comprehensive | Enhanced + `utils/` (+ optional `mock/`) | Large, high-complexity features only |

For full folder tree examples for each tier → read [references/feature-levels.md](references/feature-levels.md)

## Keep inside the feature vs move to shared `src/*`

**Keep inside** when it has domain-specific naming or depends on feature logic:
`useOrderForm`, `OrderSummaryCard`, `mapOrderRealmToForm`, `orderSchema`

**Move to shared** only when it is reused across multiple features and has no domain dependency:
`Button`, `EmptyState`, `formatCurrency`, `useDebouncedValue`

Domain language in the name is a strong signal it stays inside.

## `realm/` always needs all three subfolders

When a feature has a `realm/` folder, it must contain:
- `schemas/` — Realm schema definitions
- `services/` — read/write operations (small and focused per operation)
- `adapters/` — explicit mappers between Realm objects, API DTOs, and form models

Missing any of these is a smell. Adapters are especially important: they prevent Realm objects and remote DTOs from leaking directly into screens and forms.

For the full layout and naming conventions → read [references/realm-structure.md](references/realm-structure.md)

## `api/` layout

`api/` contains `dtos/` (TypeScript interfaces for API contracts) and `services/` (plain async functions — no query hooks). Never treat `api/` as the runtime source of truth; it feeds data into Realm through adapters.

No TanStack Query anywhere in this app — it conflicts with the Realm-first pattern.

For the full layout → read [references/api-structure.md](references/api-structure.md)

## Module folders and index exports

Related features group under a module folder (`accessControl/`, `operational/`). A module folder has no code of its own — only feature subfolders inside it.

For current module structure → read [references/module-pattern.md](references/module-pattern.md)

Every feature has a root `index.ts`. Always export screens from it. Export `./api` when present. Never export `realm/` by default.

For index export patterns → read [references/index-exports.md](references/index-exports.md)

## Offline-first consistency checks

Flag these explicitly and suggest the smallest correction:

- `pages/` used instead of `screens/`
- TanStack Query anywhere in the feature
- Feature persists domain data but has no `realm/` folder
- `api/` used as runtime source of truth instead of Realm
- `realm/` present but missing `schemas/`, `services/`, or `adapters/`
- Adapters placed outside `realm/adapters/`
- `realm/` exported from the feature root without a deliberate reason
- Screens calling remote API directly
- Components containing persistence or sync logic

## Output format

1. Propose the folder tree (smallest valid for the chosen tier)
2. Explain which folders are included and why
3. List any inconsistencies found with the smallest correction for each
4. Note if anything should move to shared `src/*`, only if real reuse is clear
