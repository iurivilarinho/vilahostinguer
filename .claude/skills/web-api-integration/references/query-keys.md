# Query Key Conventions

Query keys must be **stable, predictable, and composed of primitive values**.

Never use a plain object as a query key — React Query compares keys by deep equality, but object keys make partial invalidation harder and introduce subtle caching bugs.

## Non-paginated keys

```ts
// Lists
["clients"]["users"]["entities"][
  // Single entity
  ("client", id)
][("user", id)][("entity", id)][
  // Relations
  ("client-users", clientId)
];
```

## Paginated keys

Destructure params before building the key so each primitive is individually stable:

```ts
const { page, size, sort, filter } = params ?? {};

queryKey: ["entities", page, size, sort, filter];
```

This allows React Query to invalidate precisely — e.g., `queryClient.invalidateQueries({ queryKey: ["entities"] })` invalidates all pages of that entity list.

## Rules

- Query keys must include **all parameters** that affect the response
- Query keys must use **flattened primitives** — never `["entities", params]`
- The same key structure must be used in both the hook and at invalidation sites
- Always destructure params in the hook before spreading into the key
