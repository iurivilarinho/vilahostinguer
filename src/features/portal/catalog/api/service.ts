import { useQuery } from "@tanstack/react-query";
import { portalApi } from "@/lib/api/clients";
import { catalogKeys } from "./keys";
import type { PortalInfoDto, PortalPlanDto } from "./types";

const getInfo = async (): Promise<PortalInfoDto> => {
  const { data } = await portalApi.get<PortalInfoDto>("/public/info");
  return data;
};

const getPlans = async (): Promise<PortalPlanDto[]> => {
  const { data } = await portalApi.get<PortalPlanDto[]>("/public/plans");
  return data;
};

export const usePortalInfoQuery = () =>
  useQuery({
    queryKey: catalogKeys.info(),
    queryFn: getInfo,
    staleTime: 5 * 60_000,
  });

export const usePlansQuery = () =>
  useQuery({
    queryKey: catalogKeys.plans(),
    queryFn: getPlans,
  });
