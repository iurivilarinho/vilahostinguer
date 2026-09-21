import type { UseQueryOptions } from "@tanstack/react-query";

/** Opções de query liberadas para quem chama: a chave é sempre do hook. */
export type QueryOptions<TData> = Omit<UseQueryOptions<TData>, "queryKey" | "queryFn">;

export type ApiRequestSortParam<T> = {
  by: keyof T;
  direction: "asc" | "desc";
};

export type ApiRequestParams<T extends object = Record<string, unknown>, TFilter = unknown> = {
  page?: number;
  size?: number;
  filter?: TFilter;
  sort?: ApiRequestSortParam<T>[];
};

export type PaginatedApiResponse<T> = {
  data: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

/** Formato do `Page<T>` do Spring, normalizado para `PaginatedApiResponse` nos fetchers. */
export type SpringPage<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type TablePagination = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type MutationOptions<TData = unknown, TVariables = unknown> = {
  successMessage?: string | ((data: TData, variables: TVariables) => string);
  errorMessage?: string;
  showToast?: boolean;
  onSuccess?: (data: TData, variables: TVariables) => void;
  onError?: (error: Error) => void;
};

export type ApiErrorBody = {
  timestamp?: string;
  message?: string[];
};
