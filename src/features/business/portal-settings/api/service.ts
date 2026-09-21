import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { api } from "@/lib/api/clients";
import type { MutationOptions } from "@/lib/api/types";
import type { PortalSettingsDto, PortalSettingsRequest } from "./types";

export const portalSettingsKeys = {
  all: ["portal-settings"] as const,
};

const ISSUING_REFRESH_MS = 3_000;

const getSettings = async (): Promise<PortalSettingsDto> => (await api.get<PortalSettingsDto>("/portal-settings")).data;
const saveSettings = async (payload: PortalSettingsRequest): Promise<PortalSettingsDto> => (await api.put<PortalSettingsDto>("/portal-settings", payload)).data;
const issueCertificate = async (): Promise<PortalSettingsDto> => (await api.post<PortalSettingsDto>("/portal-settings/certificate")).data;

/** Enquanto o certificado está sendo emitido, a situação se atualiza sozinha. */
export const usePortalSettingsQuery = () =>
  useQuery({
    queryKey: portalSettingsKeys.all,
    queryFn: getSettings,
    refetchInterval: (query) => (query.state.data?.certificateStatus === "ISSUING" ? ISSUING_REFRESH_MS : false),
  });

export const useSavePortalSettingsMutation = (options?: MutationOptions<PortalSettingsDto, PortalSettingsRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: saveSettings,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(portalSettingsKeys.all, data);
      queryClient.invalidateQueries({ queryKey: ["remote-access"] });
      if (options?.showToast !== false) {
        notify.success("Painel do cliente salvo", data.enabled ? `Publicado em ${data.hostname}` : "O painel não está publicado.");
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível salvar"));
      }
      options?.onError?.(error);
    },
  });
};

export const useIssueCertificateMutation = (options?: MutationOptions<PortalSettingsDto, void>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: issueCertificate,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(portalSettingsKeys.all, data);
      if (options?.showToast !== false) {
        notify.info("Pedindo o certificado ao Let's Encrypt", "Leva de alguns segundos a um minuto.");
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível pedir o certificado"));
      }
      options?.onError?.(error);
    },
  });
};
