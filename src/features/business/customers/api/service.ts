import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notify } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { api } from "@/lib/api/clients";
import type { MutationOptions, PaginatedApiResponse, QueryOptions, SpringPage } from "@/lib/api/types";
import { normalizeSpringPage } from "@/lib/api/utils/normalize-spring-page";
import { resolveSuccessMessage } from "@/lib/api/utils/resolve-success-message";
import { toPageParams } from "@/lib/api/utils/to-page-params";
import { customerKeys } from "./keys";
import type { CustomerDto, CustomerPasswordRequest, CustomerStatusRequest, GetCustomersParams, UpdateCustomerRequest } from "./types";

const getCustomers = async (params?: GetCustomersParams): Promise<PaginatedApiResponse<CustomerDto>> => {
  const { data } = await api.get<SpringPage<CustomerDto>>("/customers", { params: toPageParams(params) });
  return normalizeSpringPage(data);
};

const updateCustomer = async ({ id, ...payload }: UpdateCustomerRequest): Promise<CustomerDto> =>
  (await api.put<CustomerDto>(`/customers/${id}`, payload)).data;

const changeStatus = async ({ id, status, reason }: CustomerStatusRequest): Promise<CustomerDto> =>
  (await api.patch<CustomerDto>(`/customers/${id}/status`, { status, reason })).data;

const resetPassword = async ({ id, password }: CustomerPasswordRequest): Promise<CustomerDto> =>
  (await api.post<CustomerDto>(`/customers/${id}/password`, { password })).data;

export const useCustomersQuery = (params?: GetCustomersParams, options?: QueryOptions<PaginatedApiResponse<CustomerDto>>) =>
  useQuery({ queryKey: customerKeys.list(params), queryFn: () => getCustomers(params), ...options });

const useCustomerMutation = <TVariables,>(
  mutationFn: (variables: TVariables) => Promise<CustomerDto>,
  defaultMessage: (data: CustomerDto) => string,
  errorMessage: string,
  options?: MutationOptions<CustomerDto, TVariables>,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: customerKeys.all });
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

export const useUpdateCustomerMutation = (options?: MutationOptions<CustomerDto, UpdateCustomerRequest>) =>
  useCustomerMutation(updateCustomer, (data) => `${data.name} salvo`, "Não foi possível salvar o cliente", options);

export const useCustomerStatusMutation = (options?: MutationOptions<CustomerDto, CustomerStatusRequest>) =>
  useCustomerMutation(changeStatus, (data) => `${data.name}: ${data.statusDescription.toLowerCase()}`, "Não foi possível alterar o cliente", options);

export const useResetCustomerPasswordMutation = (options?: MutationOptions<CustomerDto, CustomerPasswordRequest>) =>
  useCustomerMutation(resetPassword, (data) => `Senha de ${data.name} definida`, "Não foi possível definir a senha", options);
