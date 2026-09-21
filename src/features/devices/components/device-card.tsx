import { Cable, MonitorCog, Network, Server } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Card, Progress, Typography } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { formatBytes, formatRelative, percent } from "@/lib/format";
import { useDeviceMetricsQuery, type DeviceDto } from "../api";
import { DeviceStatusBadge, OnlineBadge } from "./device-status-badge";

type DeviceCardProps = {
  device: DeviceDto;
};

/** Cartão da grade de dispositivos, no estilo da lista de servidores dos painéis de hospedagem. */
export const DeviceCard = ({ device }: DeviceCardProps) => {
  const live = device.online && device.status === "READY" && device.active;
  const { data: metrics } = useDeviceMetricsQuery(device.id, { enabled: live });
  const system = [device.osName, device.osVersion].filter(Boolean).join(" ");

  return (
    <Link to={Rotas.devices.detail(device.id)} className="group block rounded-lg focus-visible:outline-2">
      <Card className="flex h-full flex-col gap-4 p-5 transition-shadow group-hover:shadow-md">
        <div className="flex items-start gap-3">
          <div className="flex size-11 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary">
            <Server className="size-5" />
          </div>
          <div className="flex min-w-0 flex-1 flex-col">
            <Typography variant="title-sm" className="truncate" title={device.name}>
              {device.name}
            </Typography>
            <Typography variant="caption" className="truncate">
              {system || "Sistema ainda não identificado"}
            </Typography>
          </div>
          <OnlineBadge online={device.online} />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Badge tone="primary">
            {device.connectionType === "USB" ? <Cable className="size-3" /> : device.connectionType === "VIRTUAL" ? <MonitorCog className="size-3" /> : <Network className="size-3" />}
            {device.connectionTypeDescription}
          </Badge>
          <Badge>
            <span className="font-mono">
              {device.host}:{device.port}
            </span>
          </Badge>
          <DeviceStatusBadge status={device.status} label={device.statusDescription} />
        </div>

        {live && metrics ? (
          <div className="flex flex-col gap-3">
            <UsageRow label="CPU" value={metrics.cpuPercent ?? 0} detail={`${(metrics.cpuPercent ?? 0).toFixed(0)}%`} />
            <UsageRow
              label="Memória"
              value={percent(metrics.memoryUsedBytes, metrics.memoryTotalBytes)}
              detail={`${formatBytes(metrics.memoryUsedBytes)} de ${formatBytes(metrics.memoryTotalBytes)}`}
            />
            <UsageRow
              label="Disco"
              value={percent(metrics.diskUsedBytes, metrics.diskTotalBytes)}
              detail={`${formatBytes(metrics.diskUsedBytes)} de ${formatBytes(metrics.diskTotalBytes)}`}
            />
          </div>
        ) : (
          <Typography variant="caption">
            {device.online ? "Configure a credencial para ver o uso de recursos." : `Visto ${formatRelative(device.lastSeenAt)}`}
          </Typography>
        )}
      </Card>
    </Link>
  );
};

type UsageRowProps = {
  label: string;
  value: number;
  detail: string;
};

const UsageRow = ({ label, value, detail }: UsageRowProps) => (
  <div className="flex flex-col gap-1.5">
    <div className="flex items-center justify-between gap-2">
      <Typography variant="caption">{label}</Typography>
      <Typography variant="caption" className="text-foreground">
        {detail}
      </Typography>
    </div>
    <Progress value={value} aria-label={label} />
  </div>
);
