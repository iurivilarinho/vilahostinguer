# Shared API Types

These types are used across all API integrations. Always import them from the paths below — never redefine or inline them.

---

## QueryOptions

```ts
import type { QueryOptions } from "@/api/types";
```

```ts
// src/api/types/queryOptions.ts
export type QueryOptions<TData = unknown> = Omit<UseQueryOptions<TData>, "queryKey">;
```

A thin wrapper over TanStack's `UseQueryOptions` that removes `queryKey` (always controlled by the hook).

Pass as the last optional argument of every query hook.

---

## MutationOptions

```ts
import type { MutationOptions } from "@/api/types";
```

```ts
// src/api/types/mutationOptions.ts
export interface MutationOptions<TData = unknown, TVariables = unknown> {
  successMessage?: string | ((data: TData, variables: TVariables) => string);
  errorMessage?: string;
  showToast?: boolean;
  onSuccess?: (data: TData, variables: TVariables) => void;
  onError?: (error: Error) => void;
}
```

**Important:** This is a custom interface, not TanStack's `UseMutationOptions`. It controls toast orchestration and external callbacks. Toast logic lives inside the mutation hook — components receive only `onSuccess` / `onError`.

Pass as the last optional argument of every mutation hook.

---

## ApiSortParam

```ts
import type { ApiSortParam } from "@/api/dtos";
```

```ts
// src/api/dtos/checklist/common.ts
export interface ApiSortParam<T> {
  by: keyof T;
  direction: "asc" | "desc";
}
```

Used inside `ApiRequestParams.sort`. `T` must be the DTO type so `by` is constrained to valid keys.

---

## ApiRequestParams

```ts
import type { ApiRequestParams } from "@/api/dtos";
```

```ts
// src/api/dtos/checklist/common.ts
export interface ApiRequestParams<T extends object = Record<string, unknown>, TFilter = unknown> {
  page?: number;
  size?: number;
  filter?: TFilter;
  sort?: ApiSortParam<T>[];
}
```

Standard envelope for all paginated requests. `T` is the DTO shape (constrains sort keys), `TFilter` is the feature-specific filter shape.

**Usage pattern:**

```ts
interface GetUsersFilter {
  search?: string;
  active?: boolean;
}

type GetUsersParams = ApiRequestParams<UserApiDto, GetUsersFilter>;
```

---

## ApiPaginatedResponse

```ts
import type { ApiPaginatedResponse } from "@/api/dtos";
```

```ts
// src/api/dtos/checklist/common.ts
export interface ApiPaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;        // 0-based current page number
  numberOfElements: number;
  empty: boolean;
  pageable: { pageNumber: number; pageSize: number; offset: number; ... };
  sort: { empty: boolean; sorted: boolean; unsorted: boolean };
}
```

Spring Boot `Page<T>` shape. The useful fields for UI are: `content`, `totalElements`, `totalPages`, `number`, `size`.

Pages are **0-based** — `number: 0` is the first page. This matches the `usePagination` hook.
