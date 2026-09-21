import { Badge, Tooltip } from "@/components";
import type { VolumeDto, VolumeStatus } from "../api";

const TONES: Record<VolumeStatus, "neutral" | "primary" | "success" | "warning" | "destructive" | "info"> = {
  AVAILABLE: "neutral",
  ATTACHING: "info",
  ATTACHED: "success",
  WAITING: "warning",
  DETACHING: "info",
  FAILED: "destructive",
  DELETED: "neutral",
};

type VolumeStatusBadgeProps = {
  volume: VolumeDto;
};

export const VolumeStatusBadge = ({ volume }: VolumeStatusBadgeProps) => {
  const badge = <Badge tone={TONES[volume.status]}>{volume.statusDescription}</Badge>;
  return volume.statusMessage ? <Tooltip content={volume.statusMessage}>{badge}</Tooltip> : badge;
};
