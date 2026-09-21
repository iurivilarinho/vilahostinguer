import type { GetDomainsParams, GetRoutesParams } from "./types";

export const remoteAccessKeys = {
  all: ["remote-access"] as const,
  domains: () => [...remoteAccessKeys.all, "domains"] as const,
  domainList: (params?: GetDomainsParams) =>
    [...remoteAccessKeys.domains(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
  routes: () => [...remoteAccessKeys.all, "routes"] as const,
  routeList: (params?: GetRoutesParams) =>
    [...remoteAccessKeys.routes(), params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
  gateway: () => [...remoteAccessKeys.all, "gateway"] as const,
};
