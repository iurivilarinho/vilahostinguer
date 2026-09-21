import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { operationKeys } from "./keys";
import type { GetOperationsParams, OperationBasicDto, OperationDto } from "./types";

const RUNNING_REFRESH_MS = 1_000;

const getOperations = async (params?: GetOperationsParams): Promise<PaginatedApiResponse<OperationBasicDto>> => {
  const { data } = await api.get<SpringPage<OperationBasicDto>>("/operations", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const getOperation = async (id: number): Promise<OperationDto> => {
  const { data } = await api.get<OperationDto>(`/operations/${id}`);
  return data;
};

const cancelOperation = async (id: number): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/operations/${id}/cancel`);
  return data;
};

export const useOperationsQuery = (params?: GetOperationsParams, options?: QueryOptions<PaginatedApiResponse<OperationBasicDto>>) =>
  useQuery({
    queryKey: operationKeys.list(params),
    queryFn: () => getOperations(params),
    ...options,
  });

/** Enquanto a operação roda, relê a cada segundo para a saída aparecer ao vivo. */
export const useOperationQuery = (id?: number, options?: QueryOptions<OperationDto>) =>
  useQuery({
    queryKey: operationKeys.detail(id ?? 0),
    queryFn: () => getOperation(id ?? 0),
    enabled: id !== undefined && (options?.enabled ?? true),
    refetchInterval: (query) => (query.state.data?.finished ? false : RUNNING_REFRESH_MS),
    ...options,
  });

export const useCancelOperationMutation = (options?: MutationOptions<OperationDto, number>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: cancelOperation,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Cancelamento pedido" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível cancelar"));
      }
      options?.onError?.(error);
    },
  });
};
