# Data Mapping

## When to extract

Extract API → form mapping into a pure utility function when the transformation is non-trivial: type conversions (numbers to strings), nested field remapping, or derived fields (e.g. `itemKind` inferred from API data).

Leave it inline (`useEffect` with `reset`) when the mapping is a direct field-by-field copy with no transformation.

## Where to put it

In the feature's `utils/` folder:

```text
features/my-feature/
  utils/
    myEntityFormMapper.ts
```

Only create `utils/` if there is real content. Do not create it for trivial mappings.

## Naming

```ts
// Pure function — receives the API DTO, returns the form values object
export function mapXxxToForm(data: XxxApiDto): XxxFormValues { ... }
```

## Pattern

```ts
// utils/myEntityFormMapper.ts
import type { MyEntityApiDto } from "../api/dtos/myEntity";
import type { MyEntityFormValues } from "../pages/MyEntityFormPage";

export function mapMyEntityToForm(data: MyEntityApiDto): MyEntityFormValues {
  return {
    name: data.name,
    description: data.description ?? "",
    // explicit field-by-field — never spread the DTO
    nestedId: data.nested?.id ? String(data.nested.id) : undefined,
  };
}
```

Usage in the page:

```tsx
const { data } = useGetMyEntityById(id ? Number(id) : undefined);

useEffect(() => {
  if (data) reset(mapMyEntityToForm(data));
}, [data, reset]);
```

## Rules

- The function must be pure: no hooks, no side effects, no API calls
- Map fields explicitly — never spread the DTO directly (extra DTO fields bleed into the form)
- Named export only
- Return type must be the form values type
- Place it in the feature's `utils/` — never in `src/utils/` unless reused across features
