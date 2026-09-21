import type { InvoiceDto, InvoiceStatus, SubscriptionDto, SubscriptionStatus } from "@/features/portal/billing/api";
import type { ApiRequestParams } from "@/lib/api/types";

export type { InvoiceDto, InvoiceStatus, SubscriptionDto, SubscriptionStatus };

export type AdminSubscriptionFilter = {
  search?: string;
  customerId?: number;
  planId?: number;
  status?: SubscriptionStatus[];
};

export type AdminInvoiceFilter = {
  customerId?: number;
  subscriptionId?: number;
  status?: InvoiceStatus[];
  overdue?: boolean;
};

export type GetAdminSubscriptionsParams = ApiRequestParams<SubscriptionDto, AdminSubscriptionFilter>;
export type GetAdminInvoicesParams = ApiRequestParams<InvoiceDto, AdminInvoiceFilter>;

export type SubscriptionStatusRequest = {
  id: number;
  status: SubscriptionStatus;
  reason: string;
};

export type InvoiceStatusRequest = {
  id: number;
  status: InvoiceStatus;
  reason: string;
};
