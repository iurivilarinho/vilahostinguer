import { CheckCircle2, CircleSlash, Clock, Loader2, XCircle } from "lucide-react";
import type { ReactNode } from "react";
import { Badge } from "@/components";
import type { OperationStatus } from "../api";

type BadgeTone = "neutral" | "info" | "success" | "destructive" | "warning";

const STATUS_STYLE: Record<OperationStatus, { tone: BadgeTone; icon: ReactNode }> = {
  PENDING: { tone: "neutral", icon: <Clock className="size-3.5" /> },
  RUNNING: { tone: "info", icon: <Loader2 className="size-3.5 animate-spin" /> },
  SUCCEEDED: { tone: "success", icon: <CheckCircle2 className="size-3.5" /> },
  FAILED: { tone: "destructive", icon: <XCircle className="size-3.5" /> },
  CANCELED: { tone: "warning", icon: <CircleSlash className="size-3.5" /> },
};

type OperationStatusBadgeProps = {
  status: OperationStatus;
  label: string;
};

export const OperationStatusBadge = ({ status, label }: OperationStatusBadgeProps) => (
  <Badge tone={STATUS_STYLE[status].tone}>
    {STATUS_STYLE[status].icon}
    {label}
  </Badge>
);
