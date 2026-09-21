import type { GetMachinesParams } from "./types";

export const machineKeys = {
  all: ["machines"] as const,
  lists: () => [...machineKeys.all, "list"] as const,
  list: (params?: GetMachinesParams) =>
    [...machineKeys.lists(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
  stats: (deviceId: number) => [...machineKeys.all, "stats", deviceId] as const,
  logs: (machineId: number) => [...machineKeys.all, "logs", machineId] as const,
  docker: (deviceId: number) => [...machineKeys.all, "docker", deviceId] as const,
  distributions: (deviceId: number) => [...machineKeys.all, "distributions", deviceId] as const,
};
