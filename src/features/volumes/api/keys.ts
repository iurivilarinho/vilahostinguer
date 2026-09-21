import type { GetVolumesParams } from "./types";

export const volumeKeys = {
  all: ["volumes"] as const,
  lists: () => [...volumeKeys.all, "list"] as const,
  list: (params?: GetVolumesParams) =>
    [...volumeKeys.lists(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
  host: () => [...volumeKeys.all, "host"] as const,
};
