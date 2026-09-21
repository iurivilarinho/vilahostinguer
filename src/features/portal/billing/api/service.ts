import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { portalApi } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { billingKeys } from "./keys";
import type {
  CancelSubscriptionRequest,
  CheckoutDto,
  CheckoutRequest,
  GetInvoicesParams,
  GetSubscriptionsParams,
  InvoiceDto,
  SubscriptionDto,
} from "./types";

const OPEN_INVOICE_REFRESH_MS = 10_000;

const getSubscriptions = async (params?: GetSubscriptionsParams): Promise<PaginatedApiResponse<SubscriptionDto>> => {
  const { data } = await portalApi.get<SpringPage<SubscriptionDto>>("/subscriptions", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const getInvoices = async (params?: GetInvoicesParams): Promise<PaginatedApiResponse<InvoiceDto>> => {
  const { data } = await portalApi.get<SpringPage<InvoiceDto>>("/invoices", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const getInvoice = async (id: number): Promise<InvoiceDto> => {
  const { data } = await portalApi.get<InvoiceDto>(`/invoices/${id}`);
  return data;
};

const checkout = async (payload: CheckoutRequest): Promise<CheckoutDto> => {
  const { data } = await portalApi.post<CheckoutDto>("/checkout", payload);
  return data;
};

const payInvoice = async (id: number): Promise<InvoiceDto> => {
  const { data } = await portalApi.post<InvoiceDto>(`/invoices/${id}/pay`);
  return data;
};

const checkInvoice = async (id: number): Promise<InvoiceDto> => {
  const { data } = await portalApi.post<InvoiceDto>(`/invoices/${id}/check`);
  return data;
};

const cancelSubscription = async ({ id, ...payload }: CancelSubscriptionRequest): Promise<SubscriptionDto> => {
  const { data } = await portalApi.post<SubscriptionDto>(`/subscriptions/${id}/cancel`, payload);
  return data;
};

const keepSubscription = async (id: number): Promise<SubscriptionDto> => {
  const { data } = await portalApi.post<SubscriptionDto>(`/subscriptions/${id}/keep`);
  return data;
};

export const useSubscriptionsQuery = (params?: GetSubscriptionsParams) =>
  useQuery({ queryKey: billingKeys.subscriptions(params), queryFn: () => getSubscriptions(params) });

export const useInvoicesQuery = (params?: GetInvoicesParams) =>
  useQuery({ queryKey: billingKeys.invoices(params), queryFn: () => getInvoices(params) });

/** Fatura em aberto se atualiza sozinha: o Pix pago aparece sem recarregar. */
export const useInvoiceQuery = (id?: number) =>
  useQuery({
    queryKey: billingKeys.invoice(id ?? 0),
    queryFn: () => getInvoice(id ?? 0),
    enabled: id !== undefined,
    refetchInterval: (query) => (query.state.data?.status === "OPEN" ? OPEN_INVOICE_REFRESH_MS : false),
  });

/** Invalida tudo o que depende de cobrança (servidores incluídos) depois de uma mudança. */
const useBillingMutation = <TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  errorMessage: string,
  successMessage: ((data: TData) => string) | null,
  options?: MutationOptions<TData, TVariables>,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: billingKeys.all });
      queryClient.invalidateQueries({ queryKey: ["portal-servers"] });
      if (successMessage && options?.showToast !== false) {
        notify.success(resolveSuccessMessage({ successMessage: options?.successMessage, data, variables, defaultMessage: successMessage(data) }));
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

export const useCheckoutMutation = (options?: MutationOptions<CheckoutDto, CheckoutRequest>) =>
  useBillingMutation(checkout, "Não foi possível concluir o pedido", () => "Pedido criado! Agora é só pagar.", options);

export const usePayInvoiceMutation = (options?: MutationOptions<InvoiceDto, number>) =>
  useBillingMutation(payInvoice, "Não foi possível gerar o Pix", null, options);

export const useCheckInvoiceMutation = (options?: MutationOptions<InvoiceDto, number>) =>
  useBillingMutation(checkInvoice, "Não foi possível conferir o pagamento", null, options);

export const useCancelSubscriptionMutation = (options?: MutationOptions<SubscriptionDto, CancelSubscriptionRequest>) =>
  useBillingMutation(
    cancelSubscription,
    "Não foi possível cancelar",
    (data) => (data.status === "CANCELED" ? "Assinatura cancelada" : "Cancelamento agendado para o fim do período"),
    options,
  );

export const useKeepSubscriptionMutation = (options?: MutationOptions<SubscriptionDto, number>) =>
  useBillingMutation(keepSubscription, "Não foi possível desfazer o cancelamento", () => "A assinatura continua renovando", options);
