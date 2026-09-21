import type { GetAdminInvoicesParams, GetAdminSubscriptionsParams } from "./types";

export const salesKeys = {
  all: ["sales"] as const,
  subscriptions: (params?: GetAdminSubscriptionsParams) =>
    [...salesKeys.all, "subscriptions", params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
  invoices: (params?: GetAdminInvoicesParams) =>
    [...salesKeys.all, "invoices", params?.page, params?.size, JSON.stringify(params?.sort ?? null), JSON.stringify(params?.filter ?? null)] as const,
};
