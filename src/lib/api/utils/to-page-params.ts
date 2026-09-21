import type { ApiRequestParams } from "../types";

type QueryValue = string | number | boolean | string[] | undefined;

/**
 * Monta os parâmetros de uma listagem paginada no formato do Spring: `page`, `size`,
 * `sort=campo,direção` e os campos do filtro achatados (vazios são descartados).
 */
export const toPageParams = <T extends object, F extends object>(params?: ApiRequestParams<T, F>): Record<string, QueryValue> => {
  const query: Record<string, QueryValue> = {};
  if (!params) {
    return query;
  }
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  if (params.sort?.length) {
    query.sort = params.sort.map((sort) => `${String(sort.by)},${sort.direction}`);
  }
  if (params.filter) {
    for (const [key, value] of Object.entries(params.filter)) {
      if (value === undefined || value === null || value === "" || (Array.isArray(value) && value.length === 0)) {
        continue;
      }
      query[key] = value as QueryValue;
    }
  }
  return query;
};
