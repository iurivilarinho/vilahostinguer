import { HardDrive, Usb } from "lucide-react";
import { Badge, Card, Skeleton, Tooltip, Typography } from "@/components";
import { formatBytes, percent } from "@/lib/format";
import type { HostDiskDto } from "../api";

type HostDisksProps = {
  disks: HostDiskDto[] | undefined;
  isLoading: boolean;
};

/**
 * Cada disco do PC numa barra: o que o Windows e os outros arquivos ocupam, o que os discos virtuais
 * já gravaram, o que eles ainda vão ocupar (reservado) e o que sobra para discos novos.
 */
export const HostDisks = ({ disks, isLoading }: HostDisksProps) => {
  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <Skeleton className="h-36 w-full" />
        <Skeleton className="h-36 w-full" />
      </div>
    );
  }
  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {(disks ?? []).map((disk) => (
        <HostDiskCard key={disk.root} disk={disk} />
      ))}
    </div>
  );
};

const HostDiskCard = ({ disk }: { disk: HostDiskDto }) => {
  const written = disk.allocatedBytes - disk.reservedBytes;
  const other = Math.max(0, disk.totalBytes - disk.freeBytes - written);
  const segments = [
    { label: "Outros arquivos", bytes: other, className: "bg-muted-foreground/40" },
    { label: "Gravado nos discos virtuais", bytes: written, className: "bg-primary" },
    { label: "Reservado para os discos virtuais", bytes: disk.reservedBytes, className: "bg-primary/35" },
  ];
  return (
    <Card className="flex flex-col gap-3 p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary">
            {disk.removable ? <Usb className="size-5" /> : <HardDrive className="size-5" />}
          </span>
          <div className="flex min-w-0 flex-col">
            <Typography variant="ui-header" className="truncate">
              {disk.root} {disk.label ? `· ${disk.label}` : ""}
            </Typography>
            <Typography variant="caption">
              {disk.fileSystem} · {formatBytes(disk.totalBytes)}
            </Typography>
          </div>
        </div>
        {disk.supported ? (
          <Badge tone="success">{formatBytes(disk.availableBytes)} para discos</Badge>
        ) : (
          <Tooltip content={disk.unsupportedReason}>
            <Badge tone="warning">Não serve</Badge>
          </Tooltip>
        )}
      </div>
      <div className="flex h-2.5 w-full overflow-hidden rounded-full bg-muted" role="img" aria-label={`Uso de ${disk.root}`}>
        {segments.map((segment) => (
          <Tooltip key={segment.label} content={`${segment.label}: ${formatBytes(segment.bytes)}`}>
            <span className={segment.className} style={{ width: `${percent(segment.bytes, disk.totalBytes)}%` }} />
          </Tooltip>
        ))}
      </div>
      <div className="flex flex-wrap gap-x-4 gap-y-1">
        <Typography variant="caption">Livre: {formatBytes(disk.freeBytes)}</Typography>
        <Typography variant="caption">Discos virtuais: {formatBytes(disk.allocatedBytes)}</Typography>
        {disk.reservedBytes > 0 && <Typography variant="caption">Ainda por ocupar: {formatBytes(disk.reservedBytes)}</Typography>}
      </div>
    </Card>
  );
};
