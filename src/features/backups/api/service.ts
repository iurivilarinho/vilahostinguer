import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { tokenStorage } from "@/app/utils/token-storage";
import { operationKeys, type OperationDto } from "@/features/operations/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { backupKeys } from "./keys";
import type { BackupDto, BackupRequest, GetBackupsParams } from "./types";

const CREATING_REFRESH_MS = 2_000;

const getBackups = async (params?: GetBackupsParams): Promise<PaginatedApiResponse<BackupDto>> => {
  const { data } = await api.get<SpringPage<BackupDto>>("/backups", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const createBackup = async (payload: BackupRequest): Promise<BackupDto> => {
  const { data } = await api.post<BackupDto>("/backups", payload);
  return data;
};

const restoreBackup = async (id: number): Promise<OperationDto> => {
  const { data } = await api.post<OperationDto>(`/backups/${id}/restore`);
  return data;
};

const discardBackup = async (id: number): Promise<BackupDto> => {
  const { data } = await api.patch<BackupDto>(`/backups/${id}/status`, { status: "DISCARDED" });
  return data;
};

export const backupDownloadUrl = (id: number): string => tokenStorage.withToken(`/api/backups/${id}/download`);

/** Enquanto houver backup sendo gerado na página, a lista se atualiza sozinha. */
export const useBackupsQuery = (params?: GetBackupsParams, options?: QueryOptions<PaginatedApiResponse<BackupDto>>) =>
  useQuery({
    queryKey: backupKeys.list(params),
    queryFn: () => getBackups(params),
    refetchInterval: (query) => (query.state.data?.data.some((backup) => backup.status === "CREATING") ? CREATING_REFRESH_MS : false),
    ...options,
  });

export const useCreateBackupMutation = (options?: MutationOptions<BackupDto, BackupRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createBackup,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: backupKeys.all });
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `Backup "${data.name}" iniciado` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível iniciar o backup"));
      }
      options?.onError?.(error);
    },
  });
};

export const useRestoreBackupMutation = (options?: MutationOptions<OperationDto, number>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: restoreBackup,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: operationKeys.all });
      if (options?.showToast !== false) {
        notify.info(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Restauração iniciada" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível restaurar"));
      }
      options?.onError?.(error);
    },
  });
};

export const useDiscardBackupMutation = (options?: MutationOptions<BackupDto, number>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: discardBackup,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: backupKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Backup descartado" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível descartar o backup"));
      }
      options?.onError?.(error);
    },
  });
};
