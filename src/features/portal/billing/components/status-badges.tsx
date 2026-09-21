import { Badge } from "@/components";
import type { InvoiceStatus, SubscriptionStatus } from "../api";

const SUBSCRIPTION_TONE: Record<SubscriptionStatus, "warning" | "info" | "success" | "destructive" | "neutral"> = {
  PENDING_PAYMENT: "warning",
  PROVISIONING: "info",
  ACTIVE: "success",
  SUSPENDED: "destructive",
  CANCELED: "neutral",
};

export const SubscriptionStatusBadge = ({ status, label }: { status: SubscriptionStatus; label: string }) => (
  <Badge tone={SUBSCRIPTION_TONE[status]}>{label}</Badge>
);

/** Em aberto e vencida aparece em vermelho, para chamar atenção. */
export const InvoiceStatusBadge = ({ status, label, overdue }: { status: InvoiceStatus; label: string; overdue: boolean }) => {
  if (status === "OPEN") {
    return <Badge tone={overdue ? "destructive" : "warning"}>{overdue ? "Vencida" : label}</Badge>;
  }
  return <Badge tone={status === "PAID" ? "success" : "neutral"}>{label}</Badge>;
};
