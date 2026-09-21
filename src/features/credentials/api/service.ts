import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { deviceKeys } from "@/features/devices/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { credentialKeys } from "./keys";
import type {
  ChangeCredentialActiveRequest,
  CredentialDto,
  CredentialRequest,
  CredentialSecretDto,
  GetCredentialsParams,
  UpdateCredentialRequest,
} from "./types";

const getCredentials = async (params?: GetCredentialsParams): Promise<PaginatedApiResponse<CredentialDto>> => {
  const { data } = await api.get<SpringPage<CredentialDto>>("/credentials", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const createCredential = async (payload: CredentialRequest): Promise<CredentialDto> => {
  const { data } = await api.post<CredentialDto>("/credentials", payload);
  return data;
};

const updateCredential = async ({ id, ...payload }: UpdateCredentialRequest): Promise<CredentialDto> => {
  const { data } = await api.put<CredentialDto>(`/credentials/${id}`, payload);
  return data;
};

const changeCredentialActive = async ({ id, active }: ChangeCredentialActiveRequest): Promise<CredentialDto> => {
  const { data } = await api.patch<CredentialDto>(`/credentials/${id}/active`, { active });
  return data;
};

const revealCredential = async (id: number): Promise<CredentialSecretDto> => {
  const { data } = await api.post<CredentialSecretDto>(`/credentials/${id}/reveal`);
  return data;
};

export const useCredentialsQuery = (params?: GetCredentialsParams, options?: QueryOptions<PaginatedApiResponse<CredentialDto>>) =>
  useQuery({
    queryKey: credentialKeys.list(params),
    queryFn: () => getCredentials(params),
    ...options,
  });

export const useCreateCredentialMutation = (options?: MutationOptions<CredentialDto, CredentialRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createCredential,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: credentialKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Credencial guardada" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível guardar a credencial"));
      }
      options?.onError?.(error);
    },
  });
};

export const useUpdateCredentialMutation = (options?: MutationOptions<CredentialDto, UpdateCredentialRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateCredential,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: credentialKeys.all });
      queryClient.invalidateQueries({ queryKey: deviceKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Credencial atualizada" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível atualizar a credencial"));
      }
      options?.onError?.(error);
    },
  });
};

export const useChangeCredentialActiveMutation = (options?: MutationOptions<CredentialDto, ChangeCredentialActiveRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: changeCredentialActive,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: credentialKeys.all });
      if (options?.showToast !== false) {
        notify.success(
          resolveSuccessMessage({
            successMessage: options?.successMessage,
            data,
            variables,
            defaultMessage: variables.active ? "Credencial reativada" : "Credencial arquivada",
          }),
        );
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível alterar a credencial"));
      }
      options?.onError?.(error);
    },
  });
};

/** Revelar é uma ação explícita, nunca uma query em cache: o segredo não fica guardado na interface. */
export const useRevealCredentialMutation = (options?: MutationOptions<CredentialSecretDto, number>) =>
  useMutation({
    mutationFn: revealCredential,
    gcTime: 0,
    onSuccess: (data, variables) => options?.onSuccess?.(data, variables),
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível revelar o segredo"));
      }
      options?.onError?.(error);
    },
  });
