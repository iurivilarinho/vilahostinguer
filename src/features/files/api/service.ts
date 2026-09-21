import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { tokenStorage } from "@/app/utils/token-storage";
import { api } from "@/lib/api/clients";
import type { MutationOptions, QueryOptions } from "@/lib/api/types";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { fileKeys } from "./keys";
import type { FileEntryDto, FilePathRequest, FileUploadDto, FileUploadRequest } from "./types";

const getDirectory = async (deviceId: number, path: string): Promise<FileEntryDto[]> => {
  const { data } = await api.get<FileEntryDto[]>(`/devices/${deviceId}/files`, { params: { path } });
  return data;
};

const uploadFile = async ({ deviceId, path, file }: FileUploadRequest): Promise<FileUploadDto> => {
  const body = new FormData();
  body.append("path", path);
  body.append("file", file);
  const { data } = await api.post<FileUploadDto>(`/devices/${deviceId}/files/upload`, body);
  return data;
};

const createFolder = async ({ deviceId, path }: FilePathRequest): Promise<void> => {
  await api.post(`/devices/${deviceId}/files/folders`, { path });
};

const deleteEntry = async ({ deviceId, path }: FilePathRequest): Promise<void> => {
  await api.delete(`/devices/${deviceId}/files`, { params: { path } });
};

/** Endereço de download direto (o navegador salva o arquivo; o token vai na URL). */
export const fileDownloadUrl = (deviceId: number, path: string): string =>
  tokenStorage.withToken(`/api/devices/${deviceId}/files/download?path=${encodeURIComponent(path)}`);

export const useDirectoryQuery = (deviceId?: number, path?: string, options?: QueryOptions<FileEntryDto[]>) =>
  useQuery({
    queryKey: fileKeys.directory(deviceId ?? 0, path ?? "/"),
    queryFn: () => getDirectory(deviceId ?? 0, path ?? "/"),
    enabled: deviceId !== undefined && (options?.enabled ?? true),
    retry: false,
    ...options,
  });

export const useUploadFileMutation = (options?: MutationOptions<FileUploadDto, FileUploadRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: uploadFile,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: fileKeys.directory(variables.deviceId, variables.path) });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `Enviado: ${data.path}` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível enviar o arquivo"));
      }
      options?.onError?.(error);
    },
  });
};

export const useCreateFolderMutation = (options?: MutationOptions<void, FilePathRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createFolder,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: fileKeys.device(variables.deviceId) });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Pasta criada" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível criar a pasta"));
      }
      options?.onError?.(error);
    },
  });
};

export const useDeleteEntryMutation = (options?: MutationOptions<void, FilePathRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteEntry,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: fileKeys.device(variables.deviceId) });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Apagado" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível apagar"));
      }
      options?.onError?.(error);
    },
  });
};
