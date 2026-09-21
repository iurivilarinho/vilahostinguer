import type { GetCredentialsParams } from "./types";

export const credentialKeys = {
  all: ["credentials"] as const,
  lists: () => [...credentialKeys.all, "list"] as const,
  list: (params?: GetCredentialsParams) =>
    [...credentialKeys.lists(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
};
