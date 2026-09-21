import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { operationKeys, type OperationDto } from "@/features/operations/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { deviceKeys } from "./keys";
import type {
  ChangeDeviceActiveRequest,
  DeviceDto,
  DeviceMetricsDto,
  DeviceRequest,
  GetDevicesParams,
  ScanStatusDto,
  UpdateDeviceRequest,
} from "./types";

const METRICS_REFRESH_MS = 5_000;

const getDevices = async (params?: GetDevicesParams): Promise<PaginatedApiResponse<DeviceDto>> => {
  const { data } = await api.get<SpringPage<DeviceDto>>("/devices", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const getDevice = async (id: number): Promise<DeviceDto> => {
  const { data } = await api.get<DeviceDto>(`/devices/${id}`);
  return data;
};

const getDeviceMetrics = async (id: number): Promise<DeviceMetricsDto> => {
  const { data } = await api.get<DeviceMetricsDto>(`/devices/${id}/metrics`);
  return data;
};

const createDevice = async (payload: DeviceRequest): Promise<DeviceDto> => {
  const { data } = await api.post<DeviceDto>("/devices", payload);
  return data;
};

const updateDevice = async ({ id, ...payload }: UpdateDeviceRequest): Promise<DeviceDto> => {
  const { data } = await api.put<DeviceDto>(`/devices/${id}`, payload);
  return data;
};

const changeDeviceActive = async ({ id, active }: ChangeDeviceActiveRequest): Promise<DeviceDto> => {
  const { data } = await api.patch<DeviceDto>(`/devices/${id}/active`, { active });
  return data;
};

const refreshDeviceFacts = async (id: number): Promise<DeviceDto> => {
  const { data } = await api.post<DeviceDto>(`/devices/${id}/facts`);
  return data;
};

const upgradeDevice = async (id: number): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/devices/${id}/upgrade`);
  return data;
};

const getScanStatus = async (): Promise<ScanStatusDto> => {
  const { data } = await api.get<ScanStatusDto>("/discovery/status");
  return data;
};

const scanNow = async (): Promise<ScanStatusDto> => {
  const { data } = await api.post<ScanStatusDto>("/discovery/scan");
  return data;
};

export const useDevicesQuery = (params?: GetDevicesParams, options?: QueryOptions<PaginatedApiResponse<DeviceDto>>) =>
  useQuery({
    queryKey: deviceKeys.list(params),
    queryFn: () => getDevices(params),
    ...options,
  });

export const useDeviceQuery = (id?: number, options?: QueryOptions<DeviceDto>) =>
  useQuery({
    queryKey: deviceKeys.detail(id ?? 0),
    queryFn: () => getDevice(id ?? 0),
    enabled: id !== undefined && (options?.enabled ?? true),
    ...options,
  });

/** Uso de recursos relido a cada 5 s enquanto a tela estiver aberta. */
export const useDeviceMetricsQuery = (id?: number, options?: QueryOptions<DeviceMetricsDto>) =>
  useQuery({
    queryKey: deviceKeys.metrics(id ?? 0),
    queryFn: () => getDeviceMetrics(id ?? 0),
    enabled: id !== undefined && (options?.enabled ?? true),
    refetchInterval: METRICS_REFRESH_MS,
    retry: false,
    ...options,
  });

export const useScanStatusQuery = (options?: QueryOptions<ScanStatusDto>) =>
  useQuery({
    queryKey: deviceKeys.scan(),
    queryFn: getScanStatus,
    refetchInterval: 10_000,
    ...options,
  });

export const useCreateDeviceMutation = (options?: MutationOptions<DeviceDto, DeviceRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createDevice,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: deviceKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Dispositivo cadastrado" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível cadastrar o dispositivo"));
      }
      options?.onError?.(error);
    },
  });
};

export const useUpdateDeviceMutation = (options?: MutationOptions<DeviceDto, UpdateDeviceRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateDevice,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: deviceKeys.all });
      if (options?.showToast !== false) {
        notify.success(
          resolveSuccessMessage({
            successMessage: options?.successMessage,
            data,
            variables,
            defaultMessage: data.status === "READY" ? "Dispositivo salvo e acessível" : "Dispositivo salvo",
          }),
        );
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível salvar o dispositivo"));
      }
      options?.onError?.(error);
    },
  });
};

export const useChangeDeviceActiveMutation = (options?: MutationOptions<DeviceDto, ChangeDeviceActiveRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: changeDeviceActive,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: deviceKeys.all });
      if (options?.showToast !== false) {
        notify.success(
          resolveSuccessMessage({
            successMessage: options?.successMessage,
            data,
            variables,
            defaultMessage: variables.active ? "Dispositivo reativado" : "Dispositivo arquivado",
          }),
        );
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível alterar o dispositivo"));
      }
      options?.onError?.(error);
    },
  });
};

export const useRefreshDeviceFactsMutation = (options?: MutationOptions<DeviceDto, number>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: refreshDeviceFacts,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(deviceKeys.detail(data.id), data);
      queryClient.invalidateQueries({ queryKey: deviceKeys.lists() });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Informações atualizadas" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      queryClient.invalidateQueries({ queryKey: deviceKeys.all });
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível ler o dispositivo"));
      }
      options?.onError?.(error);
    },
  });
};

export const useUpgradeDeviceMutation = (options?: MutationOptions<OperationDto, number>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: upgradeDevice,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Atualização do sistema iniciada" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível iniciar a atualização"));
      }
      options?.onError?.(error);
    },
  });
};

export const useScanNowMutation = (options?: MutationOptions<ScanStatusDto, void>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: scanNow,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(deviceKeys.scan(), data);
      queryClient.invalidateQueries({ queryKey: deviceKeys.all });
      if (options?.showToast !== false) {
        notify.success(
          resolveSuccessMessage({
            successMessage: options?.successMessage,
            data,
            variables,
            defaultMessage: `Busca concluída: ${data.reachableHosts.length} endereço(s) com SSH`,
          }),
        );
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível procurar dispositivos"));
      }
      options?.onError?.(error);
    },
  });
};
