import type { GetOperationsParams } from "./types";

export const operationKeys = {
  all: ["operations"] as const,
  lists: () => [...operationKeys.all, "list"] as const,
  list: (params?: GetOperationsParams) =>
    [...operationKeys.lists(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
  detail: (id: number) => [...operationKeys.all, "detail", id] as const,
};
