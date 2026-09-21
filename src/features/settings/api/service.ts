import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { deviceKeys } from "@/features/devices/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, QueryOptions } from "@/lib/api/types";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { settingsKeys } from "./keys";
import type { SettingsDto, SettingsRequest } from "./types";

const getSettings = async (): Promise<SettingsDto> => {
  const { data } = await api.get<SettingsDto>("/settings");
  return data;
};

const updateSettings = async (payload: SettingsRequest): Promise<SettingsDto> => {
  const { data } = await api.put<SettingsDto>("/settings", payload);
  return data;
};

export const useSettingsQuery = (options?: QueryOptions<SettingsDto>) =>
  useQuery({
    queryKey: settingsKeys.all,
    queryFn: getSettings,
    ...options,
  });

export const useUpdateSettingsMutation = (options?: MutationOptions<SettingsDto, SettingsRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateSettings,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(settingsKeys.all, data);
      queryClient.invalidateQueries({ queryKey: deviceKeys.scan() });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Configurações salvas" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível salvar as configurações"));
      }
      options?.onError?.(error);
    },
  });
};
