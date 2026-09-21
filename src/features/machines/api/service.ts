import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { backupKeys, type BackupDto } from "@/features/backups/api";
import { operationKeys, type OperationDto } from "@/features/operations/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { machineKeys } from "./keys";
import type {
  DistributionDto,
  DockerStatusDto,
  GetMachinesParams,
  MachineActionRequest,
  MachineBackupRequest,
  MachineCreationDto,
  MachineDto,
  MachineLogsDto,
  MachineReinstallRequest,
  MachineRequest,
  MachineRestoreRequest,
  MachineStatsDto,
} from "./types";

const LIVE_REFRESH_MS = 5_000;
const CREATING_REFRESH_MS = 3_000;

const getMachines = async (params?: GetMachinesParams): Promise<PaginatedApiResponse<MachineDto>> => {
  const { data } = await api.get<SpringPage<MachineDto>>("/machines", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const getDockerStatus = async (deviceId: number): Promise<DockerStatusDto> => {
  const { data } = await api.get<DockerStatusDto>(`/devices/${deviceId}/docker`);
  return data;
};

const getDistributions = async (deviceId: number): Promise<DistributionDto[]> => {
  const { data } = await api.get<DistributionDto[]>(`/devices/${deviceId}/machine-distributions`);
  return data;
};

const getMachineStats = async (deviceId: number): Promise<MachineStatsDto[]> => {
  const { data } = await api.get<MachineStatsDto[]>(`/devices/${deviceId}/machine-stats`);
  return data;
};

const getMachineLogs = async (machineId: number): Promise<MachineLogsDto> => {
  const { data } = await api.get<MachineLogsDto>(`/machines/${machineId}/logs`);
  return data;
};

const createMachine = async (payload: MachineRequest): Promise<MachineCreationDto> => {
  const { data } = await api.post<MachineCreationDto>("/machines", payload);
  return data;
};

const runMachineAction = async ({ id, action }: MachineActionRequest): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/machines/${id}/action`, { action });
  return data;
};

const removeMachine = async (id: number): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/machines/${id}/remove`);
  return data;
};

const syncMachines = async (deviceId: number): Promise<void> => {
  await api.post(`/devices/${deviceId}/machines/sync`);
};

/** Enquanto alguma máquina estiver sendo criada, a lista se atualiza sozinha. */
export const useMachinesQuery = (params?: GetMachinesParams, options?: QueryOptions<PaginatedApiResponse<MachineDto>>) =>
  useQuery({
    queryKey: machineKeys.list(params),
    queryFn: () => getMachines(params),
    refetchInterval: (query) => (query.state.data?.data.some((machine) => machine.status === "CREATING") ? CREATING_REFRESH_MS : false),
    ...options,
  });

export const useDockerStatusQuery = (deviceId?: number, options?: QueryOptions<DockerStatusDto>) =>
  useQuery({
    queryKey: machineKeys.docker(deviceId ?? 0),
    queryFn: () => getDockerStatus(deviceId ?? 0),
    enabled: deviceId !== undefined && (options?.enabled ?? true),
    retry: false,
    ...options,
  });

export const useDistributionsQuery = (deviceId?: number, options?: QueryOptions<DistributionDto[]>) =>
  useQuery({
    queryKey: machineKeys.distributions(deviceId ?? 0),
    queryFn: () => getDistributions(deviceId ?? 0),
    enabled: deviceId !== undefined && (options?.enabled ?? true),
    staleTime: Infinity,
    ...options,
  });

export const useMachineStatsQuery = (deviceId?: number, options?: QueryOptions<MachineStatsDto[]>) =>
  useQuery({
    queryKey: machineKeys.stats(deviceId ?? 0),
    queryFn: () => getMachineStats(deviceId ?? 0),
    enabled: deviceId !== undefined && (options?.enabled ?? true),
    refetchInterval: LIVE_REFRESH_MS,
    retry: false,
    ...options,
  });

export const useMachineLogsQuery = (machineId?: number, options?: QueryOptions<MachineLogsDto>) =>
  useQuery({
    queryKey: machineKeys.logs(machineId ?? 0),
    queryFn: () => getMachineLogs(machineId ?? 0),
    enabled: machineId !== undefined && (options?.enabled ?? true),
    gcTime: 0,
    ...options,
  });

export const useCreateMachineMutation = (options?: MutationOptions<MachineCreationDto, MachineRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createMachine,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: machineKeys.all });
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `Criando a máquina ${data.machine.name}` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível criar a máquina"));
      }
      options?.onError?.(error);
    },
  });
};

export const useMachineActionMutation = (options?: MutationOptions<OperationDto, MachineActionRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: runMachineAction,
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

export const useRemoveMachineMutation = (options?: MutationOptions<OperationDto, number>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: removeMachine,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `${data.title}: iniciado` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível remover a máquina"));
      }
      options?.onError?.(error);
    },
  });
};

const backupMachine = async ({ id, name }: MachineBackupRequest): Promise<BackupDto> => {
  const { data } = await api.post<BackupDto>(`/machines/${id}/backups`, { name });
  return data;
};

const restoreMachine = async ({ id, backupId }: MachineRestoreRequest): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/machines/${id}/restore`, { backupId });
  return data;
};

const reinstallMachine = async ({ id, ...payload }: MachineReinstallRequest): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/machines/${id}/reinstall`, payload);
  return data;
};

/** Operações longas de manutenção: a lista de máquinas e a de backups se atualizam ao começar. */
const useMaintenanceMutation = <TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  defaultMessage: (data: TData) => string,
  errorMessage: string,
  options?: MutationOptions<TData, TVariables>,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: machineKeys.all });
      queryClient.invalidateQueries({ queryKey: backupKeys.all });
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: defaultMessage(data) }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? errorMessage));
      }
      options?.onError?.(error);
    },
  });
};

export const useBackupMachineMutation = (options?: MutationOptions<BackupDto, MachineBackupRequest>) =>
  useMaintenanceMutation(backupMachine, (data) => `Backup "${data.name}" iniciado`, "Não foi possível iniciar o backup", options);

export const useRestoreMachineMutation = (options?: MutationOptions<OperationDto, MachineRestoreRequest>) =>
  useMaintenanceMutation(restoreMachine, (data) => `${data.title}: iniciado`, "Não foi possível restaurar a máquina", options);

export const useReinstallMachineMutation = (options?: MutationOptions<OperationDto, MachineReinstallRequest>) =>
  useMaintenanceMutation(reinstallMachine, (data) => `${data.title}: iniciado`, "Não foi possível reinstalar a máquina", options);

/** Relê no Docker a situação real das máquinas (alguém pode ter parado um contêiner por fora). */
export const useSyncMachinesMutation = (options?: MutationOptions<void, number>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: syncMachines,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: machineKeys.lists() });
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => options?.onError?.(error),
  });
};
