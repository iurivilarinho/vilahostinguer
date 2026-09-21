# Query Hook Pattern

## API function

```ts
const getEntities = async (): Promise<EntityApiDto[]> => {
  const { data } = await apiClient.get("/entities");
  return data;
};
```

## Hook

```ts
export const useGetEntities = (options?: QueryOptions<EntityApiDto[]>) => {
  return useQuery({
    queryKey: ["entities"],
    queryFn: getEntities,
    ...options,
  });
};
```

## Rules

- API calls must be defined in a **separate async function**
- Hooks reference that function through `queryFn`
- `QueryOptions` must be spread into the query config
- Stable query keys must be used

---

# Query With Parameter

## API function

```ts
const getEntityById = async (id: number): Promise<EntityApiDto> => {
  const { data } = await apiClient.get(`/entities/${id}`);
  return data;
};
```

## Hook

```ts
export const useGetEntityById = (id?: number, options?: QueryOptions<EntityApiDto>) => {
  const enabled = !!id && (options?.enabled ?? true);

  return useQuery({
    queryKey: ["entity", id],
    queryFn: () => getEntityById(id!),
    enabled,
    ...options,
  });
};
```

## Rules

- Parameterized queries must use `enabled`
- Query keys must include all parameters
- Hooks must guard against undefined parameters

---

# Paginated Query

## Types

See `references/types.md` for `ApiRequestParams`, `ApiPaginatedResponse`, and `ApiSortParam`.

## Filter type

Define a feature-specific filter interface and compose it with `ApiRequestParams`:

```ts
interface GetEntitiesFilter {
  search?: string;
  active?: boolean;
}

type GetEntitiesParams = ApiRequestParams<EntityApiDto, GetEntitiesFilter>;
```

## API function

Build query string manually with `URLSearchParams`. Each service owns its own param-building logic:

```ts
const getEntities = async (
  params?: GetEntitiesParams,
): Promise<ApiPaginatedResponse<EntityApiDto>> => {
  const query = new URLSearchParams();
  const { page, size, sort, filter } = params ?? {};

  if (page !== undefined) query.append("page", String(page));
  if (size !== undefined) query.append("size", String(size));
  if (sort) query.append("sort", sort.map((s) => `${s.by},${s.direction}`).join(","));
  if (filter?.search) query.append("search", filter.search);
  if (filter?.active !== undefined) query.append("active", String(filter.active));

  const { data } = await checklistApi.get(`/entities?${query.toString()}`);
  return data;
};
```

## Hook

Destructure `params` before the query key so each primitive is individually stable:

```ts
export const useGetEntities = (
  params?: GetEntitiesParams,
  options?: QueryOptions<ApiPaginatedResponse<EntityApiDto>>,
) => {
  const { page, size, sort, filter } = params ?? {};

  return useQuery({
    queryKey: ["entities", page, size, sort, filter],
    queryFn: () => getEntities(params),
    ...options,
  });
};
```

## usePagination

Use the `usePagination` hook in components to manage page/size state:

```ts
import { usePagination } from "@/hooks/usePagination";

const { page, size, onPageChange, onSizeChange } = usePagination({
  initialPage: 0,
  initialSize: 10,
  resetDeps: [filter], // resets page to 0 whenever filter changes
});
```

- Pages are **0-based** (matches Spring Boot convention)
- `resetDeps` accepts any array of values — page resets to `initialPage` when any of them change
- Pass `{ page, size, filter }` directly into the query hook params

## Rules

- Paginated API functions must return `ApiPaginatedResponse<TDto>`
- Paginated hooks must accept `ApiRequestParams<TDto, TFilter>` as the first argument
- Query keys must use **flattened primitives**: `[entity, page, size, sort, filter]`
- Never use a plain params object as a query key: `["entities", params]` is not allowed
- Always destructure params before the query key — do not spread a nested object
