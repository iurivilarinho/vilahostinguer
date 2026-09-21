import type { GetDevicesParams } from "./types";

export const deviceKeys = {
  all: ["devices"] as const,
  lists: () => [...deviceKeys.all, "list"] as const,
  list: (params?: GetDevicesParams) =>
    [...deviceKeys.lists(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
  details: () => [...deviceKeys.all, "detail"] as const,
  detail: (id: number) => [...deviceKeys.details(), id] as const,
  metrics: (id: number) => [...deviceKeys.all, "metrics", id] as const,
  scan: () => ["discovery", "status"] as const,
};
