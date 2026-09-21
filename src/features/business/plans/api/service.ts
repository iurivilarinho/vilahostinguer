import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { planKeys } from "./keys";
import type { GetPlansParams, PlanDto, PlanRequest, UpdatePlanRequest } from "./types";

const getPlans = async (params?: GetPlansParams): Promise<PaginatedApiResponse<PlanDto>> => {
  const { data } = await api.get<SpringPage<PlanDto>>("/plans", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const createPlan = async (payload: PlanRequest): Promise<PlanDto> => (await api.post<PlanDto>("/plans", payload)).data;

const updatePlan = async ({ id, ...payload }: UpdatePlanRequest): Promise<PlanDto> => (await api.put<PlanDto>(`/plans/${id}`, payload)).data;

export const usePlansAdminQuery = (params?: GetPlansParams, options?: QueryOptions<PaginatedApiResponse<PlanDto>>) =>
  useQuery({ queryKey: planKeys.list(params), queryFn: () => getPlans(params), ...options });

export const useSavePlanMutation = (options?: MutationOptions<PlanDto, PlanRequest | UpdatePlanRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: PlanRequest | UpdatePlanRequest) => ("id" in payload ? updatePlan(payload) : createPlan(payload)),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: planKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `Plano ${data.name} salvo` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível salvar o plano"));
      }
      options?.onError?.(error);
    },
  });
};
