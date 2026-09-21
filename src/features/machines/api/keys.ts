import type { GetMachinesParams } from "./types";

export const machineKeys = {
  all: ["machines"] as const,
  lists: () => [...machineKeys.all, "list"] as const,
  list: (params?: GetMachinesParams) =>
    [...machineKeys.lists(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
  stats: () => [...machineKeys.all, "stats"] as const,
  logs: (machineId: number) => [...machineKeys.all, "logs", machineId] as const,
  host: () => [...machineKeys.all, "host"] as const,
  distributions: () => [...machineKeys.all, "distributions"] as const,
};
