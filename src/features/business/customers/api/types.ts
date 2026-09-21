import type { CustomerStatus, PortalCustomerDto, ProfileRequest } from "@/features/portal/session/api";
import type { ApiRequestParams } from "@/lib/api/types";

export type CustomerDto = PortalCustomerDto;

export type CustomerFilter = {
  search?: string;
  status?: CustomerStatus[];
};

export type GetCustomersParams = ApiRequestParams<CustomerDto, CustomerFilter>;

export type UpdateCustomerRequest = ProfileRequest & { id: number };

export type CustomerStatusRequest = {
  id: number;
  status: CustomerStatus;
  reason: string;
};

export type CustomerPasswordRequest = {
  id: number;
  password: string;
};

export type { CustomerStatus };
