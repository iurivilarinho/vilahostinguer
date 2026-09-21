import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { remoteAccessKeys } from "./keys";
import type {
  ChangeDomainActiveRequest,
  ChangeRouteStatusRequest,
  DomainDto,
  DomainRequest,
  GatewaySettingsRequest,
  GatewayStatusDto,
  GetDomainsParams,
  GetRoutesParams,
  RouteDto,
  RouteRequest,
  UpdateDomainRequest,
  UpdateRouteRequest,
} from "./types";

const GATEWAY_REFRESH_MS = 10_000;

const ROUTE_STATUS_MESSAGE: Record<ChangeRouteStatusRequest["status"], string> = {
  ACTIVE: "Rota reativada",
  PAUSED: "Rota pausada",
  REMOVED: "Rota removida",
};

const getDomains = async (params?: GetDomainsParams): Promise<PaginatedApiResponse<DomainDto>> => {
  const { data } = await api.get<SpringPage<DomainDto>>("/domains", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const createDomain = async (payload: DomainRequest): Promise<DomainDto> => {
  const { data } = await api.post<DomainDto>("/domains", payload);
  return data;
};

const updateDomain = async ({ id, ...payload }: UpdateDomainRequest): Promise<DomainDto> => {
  const { data } = await api.put<DomainDto>(`/domains/${id}`, payload);
  return data;
};

const changeDomainActive = async ({ id, active }: ChangeDomainActiveRequest): Promise<DomainDto> => {
  const { data } = await api.patch<DomainDto>(`/domains/${id}/active`, { active });
  return data;
};

const syncDomain = async (id: number): Promise<DomainDto> => {
  const { data } = await api.post<DomainDto>(`/domains/${id}/sync`);
  return data;
};

const getRoutes = async (params?: GetRoutesParams): Promise<PaginatedApiResponse<RouteDto>> => {
  const { data } = await api.get<SpringPage<RouteDto>>("/routes", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const createRoute = async (payload: RouteRequest): Promise<RouteDto> => {
  const { data } = await api.post<RouteDto>("/routes", payload);
  return data;
};

const updateRoute = async ({ id, ...payload }: UpdateRouteRequest): Promise<RouteDto> => {
  const { data } = await api.put<RouteDto>(`/routes/${id}`, payload);
  return data;
};

const changeRouteStatus = async ({ id, status }: ChangeRouteStatusRequest): Promise<RouteDto> => {
  const { data } = await api.patch<RouteDto>(`/routes/${id}/status`, { status });
  return data;
};

const getGatewayStatus = async (): Promise<GatewayStatusDto> => {
  const { data } = await api.get<GatewayStatusDto>("/gateway/status");
  return data;
};

const updateGatewaySettings = async (payload: GatewaySettingsRequest): Promise<GatewayStatusDto> => {
  const { data } = await api.put<GatewayStatusDto>("/gateway/settings", payload);
  return data;
};

const refreshUpnp = async (): Promise<GatewayStatusDto> => {
  const { data } = await api.post<GatewayStatusDto>("/gateway/upnp/refresh");
  return data;
};

export const useDomainsQuery = (params?: GetDomainsParams, options?: QueryOptions<PaginatedApiResponse<DomainDto>>) =>
  useQuery({
    queryKey: remoteAccessKeys.domainList(params),
    queryFn: () => getDomains(params),
    ...options,
  });

export const useRoutesQuery = (params?: GetRoutesParams, options?: QueryOptions<PaginatedApiResponse<RouteDto>>) =>
  useQuery({
    queryKey: remoteAccessKeys.routeList(params),
    queryFn: () => getRoutes(params),
    ...options,
  });

/** Portas, IP público e tráfego mudam sozinhos: a situação se atualiza a cada 10 segundos. */
export const useGatewayStatusQuery = (options?: QueryOptions<GatewayStatusDto>) =>
  useQuery({
    queryKey: remoteAccessKeys.gateway(),
    queryFn: getGatewayStatus,
    refetchInterval: GATEWAY_REFRESH_MS,
    ...options,
  });

export const useCreateDomainMutation = (options?: MutationOptions<DomainDto, DomainRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createDomain,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: remoteAccessKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: `Domínio ${data.name} cadastrado` }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível cadastrar o domínio"));
      }
      options?.onError?.(error);
    },
  });
};

export const useUpdateDomainMutation = (options?: MutationOptions<DomainDto, UpdateDomainRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateDomain,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: remoteAccessKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Domínio salvo" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível salvar o domínio"));
      }
      options?.onError?.(error);
    },
  });
};

export const useChangeDomainActiveMutation = (options?: MutationOptions<DomainDto, ChangeDomainActiveRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: changeDomainActive,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: remoteAccessKeys.all });
      if (options?.showToast !== false) {
        notify.success(
          resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: data.active ? "Domínio reativado" : "Domínio arquivado" }),
        );
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível alterar o domínio"));
      }
      options?.onError?.(error);
    },
  });
};

/** O resultado (em dia ou com falha) vem no próprio domínio; o aviso reflete isso. */
export const useSyncDomainMutation = (options?: MutationOptions<DomainDto, number>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: syncDomain,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: remoteAccessKeys.all });
      if (options?.showToast !== false) {
        if (data.lastResult === "SYNCED") {
          notify.success(`DNS de ${data.name} em dia`, data.lastMessage ?? undefined);
        } else {
          notify.error(`DNS de ${data.name} com falha`, data.lastMessage ?? undefined);
        }
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível atualizar o DNS"));
      }
      options?.onError?.(error);
    },
  });
};

export const useCreateRouteMutation = (options?: MutationOptions<RouteDto, RouteRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createRoute,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: remoteAccessKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Rota criada; a porta já está aberta" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível criar a rota"));
      }
      options?.onError?.(error);
    },
  });
};

export const useUpdateRouteMutation = (options?: MutationOptions<RouteDto, UpdateRouteRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateRoute,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: remoteAccessKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Rota salva" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível salvar a rota"));
      }
      options?.onError?.(error);
    },
  });
};

export const useChangeRouteStatusMutation = (options?: MutationOptions<RouteDto, ChangeRouteStatusRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: changeRouteStatus,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: remoteAccessKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: ROUTE_STATUS_MESSAGE[variables.status] }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível alterar a rota"));
      }
      options?.onError?.(error);
    },
  });
};

export const useUpdateGatewaySettingsMutation = (options?: MutationOptions<GatewayStatusDto, GatewaySettingsRequest>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateGatewaySettings,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(remoteAccessKeys.gateway(), data);
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: "Preferências do acesso remoto salvas" }));
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível salvar as preferências"));
      }
      options?.onError?.(error);
    },
  });
};

export const useRefreshUpnpMutation = (options?: MutationOptions<GatewayStatusDto, void>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: refreshUpnp,
    onSuccess: (data, variables) => {
      queryClient.setQueryData(remoteAccessKeys.gateway(), data);
      if (options?.showToast !== false) {
        if (data.upnp.found) {
          notify.success(`Roteador ${data.upnp.routerName ?? ""} encontrado`, `${data.upnp.mappedPorts.length} porta(s) aberta(s) nele`);
        } else {
          notify.warning("Roteador não encontrado", data.upnp.error ?? undefined);
        }
      }
      options?.onSuccess?.(data, variables);
    },
    onError: (error) => {
      if (options?.showToast !== false) {
        notify.error(getApiErrorMessage(error, options?.errorMessage ?? "Não foi possível procurar o roteador"));
      }
      options?.onError?.(error);
    },
  });
};
