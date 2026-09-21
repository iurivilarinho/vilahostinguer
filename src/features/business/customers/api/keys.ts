import type { GetCustomersParams } from "./types";

export const customerKeys = {
  all: ["customers"] as const,
  list: (params?: GetCustomersParams) =>
    [...customerKeys.all, "list", params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
};
