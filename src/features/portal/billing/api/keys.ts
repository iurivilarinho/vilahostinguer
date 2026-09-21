import type { GetInvoicesParams, GetSubscriptionsParams } from "./types";

export const billingKeys = {
  all: ["portal-billing"] as const,
  subscriptions: (params?: GetSubscriptionsParams) =>
    [...billingKeys.all, "subscriptions", params?.page, params?.size, JSON.stringify(params?.filter ?? null)] as const,
  invoices: (params?: GetInvoicesParams) =>
    [...billingKeys.all, "invoices", params?.page, params?.size, JSON.stringify(params?.filter ?? null)] as const,
  invoice: (id: number) => [...billingKeys.all, "invoice", id] as const,
};
