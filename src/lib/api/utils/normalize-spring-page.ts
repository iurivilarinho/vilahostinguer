import type { PaginatedApiResponse, SpringPage } from "../types";

export const normalizeSpringPage = <T>(page: SpringPage<T>): PaginatedApiResponse<T> => ({
  data: page.content,
  page: page.number,
  size: page.size,
  totalElements: page.totalElements,
  totalPages: page.totalPages,
  hasNext: !page.last,
  hasPrevious: !page.first,
});
