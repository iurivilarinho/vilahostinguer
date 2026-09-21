import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { machineKeys } from "@/features/machines/api";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { salesKeys } from "./keys";
import type {
  GetAdminInvoicesParams,
  GetAdminSubscriptionsParams,
  InvoiceDto,
  InvoiceStatusRequest,
  SubscriptionDto,
  SubscriptionStatusRequest,
} from "./types";

const BUSY_REFRESH_MS = 5_000;

const getSubscriptions = async (params?: GetAdminSubscriptionsParams): Promise<PaginatedApiResponse<SubscriptionDto>> =>
  normalizeSpringPage((await api.get<SpringPage<SubscriptionDto>>("/subscriptions", { params: toPageParams(params) })).data);

const getInvoices = async (params?: GetAdminInvoicesParams): Promise<PaginatedApiResponse<InvoiceDto>> =>
  normalizeSpringPage((await api.get<SpringPage<InvoiceDto>>("/invoices", { params: toPageParams(params) })).data);

const changeSubscriptionStatus = async ({ id, status, reason }: SubscriptionStatusRequest): Promise<SubscriptionDto> =>
  (await api.patch<SubscriptionDto>(`/subscriptions/${id}/status`, { status, reason })).data;

const retrySubscription = async (id: number): Promise<SubscriptionDto> => (await api.post<SubscriptionDto>(`/subscriptions/${id}/retry`)).data;

const changeInvoiceStatus = async ({ id, status, reason }: InvoiceStatusRequest): Promise<InvoiceDto> =>
  (await api.patch<InvoiceDto>(`/invoices/${id}/status`, { status, reason })).data;

/** Assinaturas sendo preparadas mudam sozinhas: a lista se atualiza enquanto houver alguma. */
export const useAdminSubscriptionsQuery = (params?: GetAdminSubscriptionsParams, options?: QueryOptions<PaginatedApiResponse<SubscriptionDto>>) =>
  useQuery({
    queryKey: salesKeys.subscriptions(params),
    queryFn: () => getSubscriptions(params),
    refetchInterval: (query) => (query.state.data?.data.some((item) => item.status === "PROVISIONING") ? BUSY_REFRESH_MS : false),
    ...options,
  });

export const useAdminInvoicesQuery = (params?: GetAdminInvoicesParams, options?: QueryOptions<PaginatedApiResponse<InvoiceDto>>) =>
  useQuery({ queryKey: salesKeys.invoices(params), queryFn: () => getInvoices(params), ...options });

const useSalesMutation = <TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  defaultMessage: (data: TData) => string,
  errorMessage: string,
  options?: MutationOptions<TData, TVariables>,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: salesKeys.all });
      queryClient.invalidateQueries({ queryKey: machineKeys.all });
      if (options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: defaultMessage(data) }));
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

export const useSubscriptionStatusMutation = (options?: MutationOptions<SubscriptionDto, SubscriptionStatusRequest>) =>
  useSalesMutation(changeSubscriptionStatus, (data) => `${data.hostname}: ${data.statusDescription.toLowerCase()}`, "Não foi possível alterar a assinatura", options);

export const useRetrySubscriptionMutation = (options?: MutationOptions<SubscriptionDto, number>) =>
  useSalesMutation(retrySubscription, (data) => `Criando ${data.hostname} de novo`, "Não foi possível tentar de novo", options);

export const useInvoiceStatusMutation = (options?: MutationOptions<InvoiceDto, InvoiceStatusRequest>) =>
  useSalesMutation(
    changeInvoiceStatus,
    (data) => (data.status === "PAID" ? `Fatura ${data.id} paga` : `Fatura ${data.id} cancelada`),
    "Não foi possível alterar a fatura",
    options,
  );
