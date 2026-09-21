import type { MachineDistribution, MachineNetworkMode, MachineStatus } from "@/features/machines/api";
import type { ApiRequestParams } from "@/lib/api/types";
import type { BillingCycle } from "../../catalog/api";

export type SubscriptionStatus = "PENDING_PAYMENT" | "PROVISIONING" | "ACTIVE" | "SUSPENDED" | "CANCELED";
export type InvoiceStatus = "OPEN" | "PAID" | "CANCELED";
export type PaymentMethod = "PIX_MERCADO_PAGO" | "MANUAL";

export type CustomerBasicDto = {
  id: number;
  name: string;
  email: string;
};

export type SubscriptionDto = {
  id: number;
  customer: CustomerBasicDto;
  planId: number;
  planName: string;
  machine: { id: number; name: string; networkMode: MachineNetworkMode; status: MachineStatus } | null;
  cycle: BillingCycle;
  cycleDescription: string;
  price: number;
  hostname: string;
  distribution: MachineDistribution;
  distributionName: string;
  version: string;
  username: string;
  status: SubscriptionStatus;
  statusDescription: string;
  provisionError: string | null;
  nextDueDate: string | null;
  cancelAtPeriodEnd: boolean;
  cancelReason: string | null;
  canceledAt: string | null;
  sshPort: number | null;
  siteHostname: string | null;
  createdAt: string;
  updatedAt: string;
};

export type InvoiceDto = {
  id: number;
  customer: CustomerBasicDto;
  subscriptionId: number;
  hostname: string;
  amount: number;
  description: string;
  renewal: boolean;
  dueDate: string;
  overdue: boolean;
  status: InvoiceStatus;
  statusDescription: string;
  paidAt: string | null;
  paymentMethod: PaymentMethod | null;
  paymentMethodDescription: string | null;
  pixCode: string | null;
  pixQrBase64: string | null;
  pixExpiresAt: string | null;
  note: string | null;
  createdAt: string;
};

export type SubscriptionFilter = {
  status?: SubscriptionStatus[];
};

export type InvoiceFilter = {
  status?: InvoiceStatus[];
  subscriptionId?: number;
};

export type GetSubscriptionsParams = ApiRequestParams<SubscriptionDto, SubscriptionFilter>;
export type GetInvoicesParams = ApiRequestParams<InvoiceDto, InvoiceFilter>;

export type CheckoutRequest = {
  planId: number;
  cycle: BillingCycle;
  hostname: string;
  distribution: MachineDistribution;
  version: string;
  username: string;
  password: string;
};

export type CheckoutDto = {
  subscription: SubscriptionDto;
  invoice: InvoiceDto;
};

export type CancelSubscriptionRequest = {
  id: number;
  atPeriodEnd: boolean;
  reason: string;
};
