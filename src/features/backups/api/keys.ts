import type { GetBackupsParams } from "./types";

export const backupKeys = {
  all: ["backups"] as const,
  lists: () => [...backupKeys.all, "list"] as const,
  list: (params?: GetBackupsParams) =>
    [...backupKeys.lists(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
};
