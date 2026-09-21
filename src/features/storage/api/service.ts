import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { operationKeys, type OperationDto } from "@/features/operations/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, QueryOptions } from "@/lib/api/types";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { storageKeys } from "./keys";
import type { FormatPartitionRequest, PartitionDto } from "./types";

const getPartitions = async (deviceId: number): Promise<PartitionDto[]> => {
  const { data } = await api.get<PartitionDto[]>(`/devices/${deviceId}/partitions`);
  return data;
};

const formatPartition = async ({ deviceId, ...payload }: FormatPartitionRequest): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/devices/${deviceId}/partitions/format`, payload);
  return data;
};

export const usePartitionsQuery = (deviceId?: number, options?: QueryOptions<PartitionDto[]>) =>
  useQuery({
    queryKey: storageKeys.device(deviceId ?? 0),
    queryFn: () => getPartitions(deviceId ?? 0),
    enabled: deviceId !== undefined && (options?.enabled ?? true),
    retry: false,
    ...options,
  });

export const useFormatPartitionMutation = (options?: MutationOptions<OperationDto, FormatPartitionRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: formatPartition,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      queryClient.invalidateQueries({ queryKey: storageKeys.device(variables.deviceId) });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `${data.title}: iniciado` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível formatar"));
      }
      options?.onError?.(error);
    },
  });
};
