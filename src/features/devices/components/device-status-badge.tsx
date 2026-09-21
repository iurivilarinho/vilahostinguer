import { Badge, StatusDot } from "@/components";
import type { DeviceStatus } from "../api";

const STATUS_TONE: Record<DeviceStatus, "success" | "warning" | "destructive"> = {
  READY: "success",
  DISCOVERED: "warning",
  AUTH_FAILED: "destructive",
};

type DeviceStatusBadgeProps = {
  status: DeviceStatus;
  label: string;
};

export const DeviceStatusBadge = ({ status, label }: DeviceStatusBadgeProps) => <Badge tone={STATUS_TONE[status]}>{label}</Badge>;

type OnlineBadgeProps = {
  online: boolean;
};

export const OnlineBadge = ({ online }: OnlineBadgeProps) => (
  <Badge tone={online ? "success" : "neutral"}>
    <StatusDot online={online} />
    {online ? "Online" : "Desconectado"}
  </Badge>
);
