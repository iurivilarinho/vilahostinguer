import type { UseQueryOptions, UseQueryResult } from "@tanstack/react-query";
import { useEffect, useMemo, useRef, useState } from "react";
import type { ApiRequestParams, ApiRequestSortParam, PaginatedApiResponse, TablePagination } from "@/lib/api/types";

type UsePaginatedDataParams<D extends object, F = unknown> = {
  query: (
    params?: ApiRequestParams<D, F>,
    options?: Omit<UseQueryOptions<PaginatedApiResponse<D>>, "queryKey">,
  ) => UseQueryResult<PaginatedApiResponse<D>>;
  filter?: F;
  sort?: ApiRequestSortParam<D>[];
  storageKey?: string;
  initialSize?: number;
  refetchInterval?: number;
};

const DEFAULT_SIZE = 25;

const readStoredSize = (storageKey: string | undefined, fallback: number): number => {
  if (!storageKey) {
    return fallback;
  }
  const stored = Number(localStorage.getItem(storageKey));
  return Number.isFinite(stored) && stored > 0 ? stored : fallback;
};

/**
 * Estado de paginação das tabelas sobre um hook de listagem do TanStack Query. Página 0-based;
 * volta para a página 0 quando filtro, ordenação ou tamanho mudam.
 */
export const usePaginatedData = <D extends object, F = unknown>({
  query,
  filter,
  sort,
  storageKey,
  initialSize = DEFAULT_SIZE,
  refetchInterval,
}: UsePaginatedDataParams<D, F>) => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(() => readStoredSize(storageKey, initialSize));

  const filterKey = JSON.stringify(filter ?? null);
  const sortKey = JSON.stringify(sort ?? null);
  const previousKeys = useRef({ filterKey, sortKey, size });

  useEffect(() => {
    const previous = previousKeys.current;
    if (previous.filterKey !== filterKey || previous.sortKey !== sortKey || previous.size !== size) {
      setPage(0);
      previousKeys.current = { filterKey, sortKey, size };
    }
  }, [filterKey, sortKey, size]);

  const result = query({ page, size, filter, sort }, { placeholderData: (previous) => previous, refetchInterval });
  const response = result.data;

  useEffect(() => {
    if (response && response.totalPages > 0 && page > response.totalPages - 1) {
      setPage(response.totalPages - 1);
    }
  }, [response, page]);

  const pagination: TablePagination = useMemo(
    () => ({
      page,
      size,
      totalElements: response?.totalElements ?? 0,
      totalPages: response?.totalPages ?? 0,
    }),
    [page, size, response],
  );

  const updatePagination = (value: Partial<TablePagination>) => {
    if (value.size !== undefined && value.size !== size) {
      setSize(value.size);
      if (storageKey) {
        localStorage.setItem(storageKey, String(value.size));
      }
    }
    if (value.page !== undefined) {
      setPage(value.page);
    }
  };

  return {
    data: response?.data ?? [],
    isLoading: result.isLoading,
    isFetching: result.isFetching,
    isError: result.isError,
    error: result.error,
    refetch: result.refetch,
    pagination,
    updatePagination,
  };
};
