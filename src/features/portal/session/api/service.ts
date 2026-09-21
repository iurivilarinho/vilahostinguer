import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { portalApi } from "@/lib/api/clients";
import type { MutationOptions } from "@/lib/api/types";
import { sessionKeys } from "./keys";
import type { ChangePasswordRequest, LoginRequest, PortalCustomerDto, ProfileRequest, RegisterRequest } from "./types";

/** Cliente logado, ou null quando não há sessão (401 não é erro aqui). */
const getMe = async (): Promise<PortalCustomerDto | null> => {
  try {
    const { data } = await portalApi.get<PortalCustomerDto>("/auth/me");
    return data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      return null;
    }
    throw error;
  }
};

const login = async (payload: LoginRequest): Promise<PortalCustomerDto> => {
  const { data } = await portalApi.post<PortalCustomerDto>("/auth/login", payload);
  return data;
};

const register = async (payload: RegisterRequest): Promise<PortalCustomerDto> => {
  const { data } = await portalApi.post<PortalCustomerDto>("/auth/register", payload);
  return data;
};

const logout = async (): Promise<void> => {
  await portalApi.post("/auth/logout");
};

const updateProfile = async (payload: ProfileRequest): Promise<PortalCustomerDto> => {
  const { data } = await portalApi.put<PortalCustomerDto>("/account", payload);
  return data;
};

const changePassword = async (payload: ChangePasswordRequest): Promise<PortalCustomerDto> => {
  const { data } = await portalApi.post<PortalCustomerDto>("/account/password", payload);
  return data;
};

export const useSessionQuery = () =>
  useQuery({
    queryKey: sessionKeys.me(),
    queryFn: getMe,
    staleTime: 60_000,
    retry: false,
  });

export const useLoginMutation = (options?: MutationOptions<PortalCustomerDto, LoginRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: login,
    onSuccess: (data, variables) => {
      queryClient.clear();
      queryClient.setQueryData(sessionKeys.me(), data);
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível entrar"));
      }
      options?.onError?.(error);
    },
  });
};

export const useRegisterMutation = (options?: MutationOptions<PortalCustomerDto, RegisterRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: register,
    onSuccess: (data, variables) => {
      queryClient.clear();
      queryClient.setQueryData(sessionKeys.me(), data);
      if (options?.showToast !== false) {
        notify.success(`Bem-vindo, ${data.name.split(" ")[0]}!`, "Sua conta foi criada.");
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível criar a conta"));
      }
      options?.onError?.(error);
    },
  });
};

export const useLogoutMutation = (options?: MutationOptions<void, void>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: logout,
    onSuccess: (data, variables) => {
      queryClient.clear();
      queryClient.setQueryData(sessionKeys.me(), null);
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => options?.onError?.(error),
  });
};

export const useUpdateProfileMutation = (options?: MutationOptions<PortalCustomerDto, ProfileRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateProfile,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(sessionKeys.me(), data);
      if (options?.showToast !== false) {
        notify.success("Dados salvos");
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível salvar os dados"));
      }
      options?.onError?.(error);
    },
  });
};

export const useChangePasswordMutation = (options?: MutationOptions<PortalCustomerDto, ChangePasswordRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: changePassword,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(sessionKeys.me(), data);
      if (options?.showToast !== false) {
        notify.success("Senha trocada", "As outras sessões abertas foram encerradas.");
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível trocar a senha"));
      }
      options?.onError?.(error);
    },
  });
};
