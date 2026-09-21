import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/clients";
import type { PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import type { AuditLogDto, GetAuditLogsParams } from "./types";

export const auditKeys = {
  all: ["audit"] as const,
  list: (params?: GetAuditLogsParams) =>
    [...auditKeys.all, params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
};

const getAuditLogs = async (params?: GetAuditLogsParams): Promise<PaginatedApiResponse<AuditLogDto>> =>
  normalizeSpringPage((await api.get<SpringPage<AuditLogDto>>("/audit-logs", { params: toPageParams(params) })).data);

export const useAuditLogsQuery = (params?: GetAuditLogsParams, options?: QueryOptions<PaginatedApiResponse<AuditLogDto>>) =>
  useQuery({ queryKey: auditKeys.list(params), queryFn: () => getAuditLogs(params), ...options });
