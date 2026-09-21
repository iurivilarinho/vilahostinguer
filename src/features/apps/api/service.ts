import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { operationKeys, type OperationDto } from "@/features/operations/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, QueryOptions } from "@/lib/api/types";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { appKeys } from "./keys";
import type { AppActionRequest, AppServiceActionRequest, DeviceAppDto } from "./types";

const getDeviceApps = async (deviceId: number): Promise<DeviceAppDto[]> => {
  const { data } = await api.get<DeviceAppDto[]>(`/devices/${deviceId}/apps`);
  return data;
};

const installApp = async ({ deviceId, app }: AppActionRequest): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/devices/${deviceId}/apps/${app}/install`);
  return data;
};

const removeApp = async ({ deviceId, app }: AppActionRequest): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/devices/${deviceId}/apps/${app}/remove`);
  return data;
};

const runServiceAction = async ({ deviceId, app, action }: AppServiceActionRequest): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/devices/${deviceId}/apps/${app}/service`, { action });
  return data;
};

export const useDeviceAppsQuery = (deviceId?: number, options?: QueryOptions<DeviceAppDto[]>) =>
  useQuery({
    queryKey: appKeys.device(deviceId ?? 0),
    queryFn: () => getDeviceApps(deviceId ?? 0),
    enabled: deviceId !== undefined && (options?.enabled ?? true),
    retry: false,
    ...options,
  });

export const useInstallAppMutation = (options?: MutationOptions<OperationDto, AppActionRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: installApp,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `${data.title}: iniciado` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível iniciar a instalação"));
      }
      options?.onError?.(error);
    },
  });
};

export const useRemoveAppMutation = (options?: MutationOptions<OperationDto, AppActionRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: removeApp,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `${data.title}: iniciado` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível iniciar a remoção"));
      }
      options?.onError?.(error);
    },
  });
};

export const useAppServiceActionMutation = (options?: MutationOptions<OperationDto, AppServiceActionRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: runServiceAction,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `${data.title}: iniciado` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível executar a ação"));
      }
      options?.onError?.(error);
    },
  });
};
