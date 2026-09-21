import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/clients";
import type { QueryOptions } from "@/lib/api/types";
import { dashboardKeys } from "./keys";
import type { DashboardSummaryDto } from "./types";

const SUMMARY_REFRESH_MS = 15_000;

const getSummary = async (): Promise<DashboardSummaryDto> => {
  const { data } = await api.get<DashboardSummaryDto>("/dashboard/summary");
  return data;
};

export const useDashboardSummaryQuery = (options?: QueryOptions<DashboardSummaryDto>) =>
  useQuery({
    queryKey: dashboardKeys.summary(),
    queryFn: getSummary,
    refetchInterval: SUMMARY_REFRESH_MS,
    ...options,
  });
