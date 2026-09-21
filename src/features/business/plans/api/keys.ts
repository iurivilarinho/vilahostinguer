import type { GetPlansParams } from "./types";

export const planKeys = {
  all: ["plans"] as const,
  list: (params?: GetPlansParams) =>
    [...planKeys.all, "list", params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
};
