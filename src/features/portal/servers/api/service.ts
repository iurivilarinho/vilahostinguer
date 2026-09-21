import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import type { DistributionDto } from "@/features/machines/api";
import { portalApi } from "@/lib/api/clients";
import type { MutationOptions } from "@/lib/api/types";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { serverKeys } from "./keys";
import type {
  ServerActionRequest,
  ServerBackupActionRequest,
  ServerBackupDto,
  ServerBackupRequest,
  ServerDto,
  ServerOperationDto,
  ServerPasswordRequest,
  ServerReinstallRequest,
  ServerStatsDto,
} from "./types";

const BUSY_REFRESH_MS = 4_000;
const STATS_REFRESH_MS = 5_000;
const OPERATION_REFRESH_MS = 1_500;

/** Servidor que ainda muda sozinho: sendo criado, preparando ou com a máquina em transição. */
const isBusy = (server: ServerDto) =>
  server.subscriptionStatus === "PROVISIONING" || server.machineStatus === "CREATING";

const getServers = async (): Promise<ServerDto[]> => (await portalApi.get<ServerDto[]>("/servers")).data;
const getServer = async (id: number): Promise<ServerDto> => (await portalApi.get<ServerDto>(`/servers/${id}`)).data;
const getStats = async (id: number): Promise<ServerStatsDto> => (await portalApi.get<ServerStatsDto>(`/servers/${id}/stats`)).data;
const getDistributions = async (id: number): Promise<DistributionDto[]> =>
  (await portalApi.get<DistributionDto[]>(`/servers/${id}/distributions`)).data;
const getBackups = async (id: number): Promise<ServerBackupDto[]> => (await portalApi.get<ServerBackupDto[]>(`/servers/${id}/backups`)).data;
const getOperation = async (id: number, operationId: number): Promise<ServerOperationDto> =>
  (await portalApi.get<ServerOperationDto>(`/servers/${id}/operations/${operationId}`)).data;

const runAction = async ({ id, action }: ServerActionRequest): Promise<ServerOperationDto> =>
  (await portalApi.post<ServerOperationDto>(`/servers/${id}/action`, { action })).data;
const reinstall = async ({ id, ...payload }: ServerReinstallRequest): Promise<ServerOperationDto> =>
  (await portalApi.post<ServerOperationDto>(`/servers/${id}/reinstall`, payload)).data;
const changePassword = async ({ id, password }: ServerPasswordRequest): Promise<void> => {
  await portalApi.post(`/servers/${id}/password`, { password });
};
const createBackup = async ({ id, name }: ServerBackupRequest): Promise<ServerBackupDto> =>
  (await portalApi.post<ServerBackupDto>(`/servers/${id}/backups`, { name })).data;
const restoreBackup = async ({ id, backupId }: ServerBackupActionRequest): Promise<ServerOperationDto> =>
  (await portalApi.post<ServerOperationDto>(`/servers/${id}/backups/${backupId}/restore`)).data;
const discardBackup = async ({ id, backupId }: ServerBackupActionRequest): Promise<ServerBackupDto> =>
  (await portalApi.post<ServerBackupDto>(`/servers/${id}/backups/${backupId}/discard`)).data;

export const backupFileUrl = (id: number, backupId: number) => `/api/portal/servers/${id}/backups/${backupId}/file`;

export const useServersQuery = () =>
  useQuery({
    queryKey: serverKeys.list(),
    queryFn: getServers,
    refetchInterval: (query) => (query.state.data?.some(isBusy) ? BUSY_REFRESH_MS : false),
  });

export const useServerQuery = (id?: number) =>
  useQuery({
    queryKey: serverKeys.detail(id ?? 0),
    queryFn: () => getServer(id ?? 0),
    enabled: id !== undefined,
    refetchInterval: (query) => (query.state.data && isBusy(query.state.data) ? BUSY_REFRESH_MS : false),
  });

export const useServerStatsQuery = (id: number, enabled: boolean) =>
  useQuery({
    queryKey: serverKeys.stats(id),
    queryFn: () => getStats(id),
    enabled,
    refetchInterval: STATS_REFRESH_MS,
    retry: false,
  });

export const useServerDistributionsQuery = (id: number, enabled = true) =>
  useQuery({ queryKey: serverKeys.distributions(id), queryFn: () => getDistributions(id), enabled, staleTime: Infinity });

export const useServerBackupsQuery = (id: number) =>
  useQuery({
    queryKey: serverKeys.backups(id),
    queryFn: () => getBackups(id),
    refetchInterval: (query) => (query.state.data?.some((backup) => backup.status === "CREATING") ? BUSY_REFRESH_MS : false),
  });

export const useServerOperationQuery = (id: number, operationId: number | null) =>
  useQuery({
    queryKey: serverKeys.operation(id, operationId ?? 0),
    queryFn: () => getOperation(id, operationId ?? 0),
    enabled: operationId !== null,
    refetchInterval: (query) => (query.state.data?.finished ? false : OPERATION_REFRESH_MS),
  });

const useServerMutation = <TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  errorMessage: string,
  successMessage: string | null,
  options?: MutationOptions<TData, TVariables>,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: serverKeys.all });
      if (successMessage && options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: successMessage }));
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

export const useServerActionMutation = (options?: MutationOptions<ServerOperationDto, ServerActionRequest>) =>
  useServerMutation(runAction, "Não foi possível executar a ação", null, options);

export const useReinstallServerMutation = (options?: MutationOptions<ServerOperationDto, ServerReinstallRequest>) =>
  useServerMutation(reinstall, "Não foi possível reinstalar", null, options);

export const useServerPasswordMutation = (options?: MutationOptions<void, ServerPasswordRequest>) =>
  useServerMutation(changePassword, "Não foi possível trocar a senha", "Senha do servidor trocada", options);

export const useCreateServerBackupMutation = (options?: MutationOptions<ServerBackupDto, ServerBackupRequest>) =>
  useServerMutation(createBackup, "Não foi possível iniciar o backup", "Backup iniciado", options);

export const useRestoreServerBackupMutation = (options?: MutationOptions<ServerOperationDto, ServerBackupActionRequest>) =>
  useServerMutation(restoreBackup, "Não foi possível restaurar", null, options);

export const useDiscardServerBackupMutation = (options?: MutationOptions<ServerBackupDto, ServerBackupActionRequest>) =>
  useServerMutation(discardBackup, "Não foi possível apagar o backup", "Backup apagado", options);
