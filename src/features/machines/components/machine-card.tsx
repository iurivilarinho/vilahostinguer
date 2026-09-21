import { Archive, Box, Disc3, FileText, Globe, MoreHorizontal, Play, Power, RotateCw, SquareTerminal, Trash2 } from "lucide-react";
import { Badge, Button, Card, DropdownMenu, Progress, Typography } from "@/components";
import { cn } from "@/lib/merge-classes";
import type { MachineDto, MachineStatsDto, MachineStatus } from "../api";

const STATUS_TONE: Record<MachineStatus, "info" | "success" | "neutral" | "destructive"> = {
  CREATING: "info",
  RUNNING: "success",
  STOPPED: "neutral",
  FAILED: "destructive",
  REMOVED: "neutral",
};

type MachineCardProps = {
  machine: MachineDto;
  stats?: MachineStatsDto;
  deviceHost: string;
  showDevice?: boolean;
  onTerminal: (machine: MachineDto) => void;
  onLogs: (machine: MachineDto) => void;
  onAction: (machine: MachineDto, action: "START" | "STOP" | "RESTART") => void;
  onRemove: (machine: MachineDto) => void;
  onPublish: (machine: MachineDto) => void;
  onBackups: (machine: MachineDto) => void;
  onReinstall: (machine: MachineDto) => void;
};

export const MachineCard = ({ machine, stats, deviceHost, showDevice = false, onTerminal, onLogs, onAction, onRemove, onPublish, onBackups, onReinstall }: MachineCardProps) => {
  const running = machine.status === "RUNNING";
  const busy = machine.status === "CREATING";
  const sshCommand = machine.sshEnabled
    ? machine.networkMode === "HOST"
      ? `ssh ${machine.username}@${deviceHost} -p ${machine.sshPort}`
      : machine.ports.find((port) => port.containerPort === machine.sshPort)
        ? `ssh ${machine.username}@${deviceHost} -p ${machine.ports.find((port) => port.containerPort === machine.sshPort)?.hostPort}`
        : null
    : null;

  return (
    <Card className="flex flex-col gap-4 p-5">
      <div className="flex items-start gap-3">
        <div className={cn("flex size-11 shrink-0 items-center justify-center rounded-lg", running ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground")}>
          <Box className="size-5" />
        </div>
        <div className="flex min-w-0 flex-1 flex-col">
          <Typography variant="title-sm" className="truncate">
            {machine.name}
          </Typography>
          <Typography variant="caption" className="truncate">
            {machine.distributionName} {machine.version}
            {showDevice ? ` · ${machine.device.name}` : ""}
          </Typography>
        </div>
        <Badge tone={STATUS_TONE[machine.status]}>{machine.statusDescription}</Badge>
      </div>

      <div className="flex flex-wrap gap-2">
        <Badge tone="primary">{machine.networkModeDescription}</Badge>
        {machine.cpuLimit !== null && <Badge>{machine.cpuLimit} CPU</Badge>}
        {machine.memoryLimitMb !== null && <Badge>{machine.memoryLimitMb} MB</Badge>}
        {machine.ports.map((port) => (
          <Badge key={`${port.hostPort}-${port.protocol}`} className="font-mono">
            {port.hostPort}→{port.containerPort}/{port.protocol}
          </Badge>
        ))}
      </div>

      {running && stats && (
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <Typography variant="caption">CPU</Typography>
            <Typography variant="caption" className="text-foreground">
              {stats.cpuPercent?.toFixed(1) ?? "—"}%
            </Typography>
          </div>
          <Progress value={stats.cpuPercent ?? 0} aria-label="CPU" />
          <div className="flex items-center justify-between">
            <Typography variant="caption">Memória</Typography>
            <Typography variant="caption" className="text-foreground">
              {stats.memoryUsage}
            </Typography>
          </div>
          <Progress value={stats.memoryPercent ?? 0} aria-label="Memória" />
        </div>
      )}

      {sshCommand && (
        <Typography variant="mono" className="truncate rounded-md bg-muted px-2 py-1 text-muted-foreground" title={sshCommand}>
          {sshCommand}
        </Typography>
      )}

      <div className="mt-auto flex items-center justify-end gap-2">
        <Button size="sm" onClick={() => onTerminal(machine)} disabled={!running}>
          <SquareTerminal />
          Terminal
        </Button>
        {running ? (
          <Button size="sm" variant="outline" onClick={() => onAction(machine, "STOP")}>
            <Power />
            Desligar
          </Button>
        ) : (
          <Button size="sm" variant="outline" onClick={() => onAction(machine, "START")} disabled={busy}>
            <Play />
            Ligar
          </Button>
        )}
        <DropdownMenu
          trigger={
            <Button variant="ghost" size="icon" aria-label={`Mais ações de ${machine.name}`}>
              <MoreHorizontal />
            </Button>
          }
          items={[
            { label: "Reiniciar", icon: <RotateCw />, disabled: !running, onSelect: () => onAction(machine, "RESTART") },
            { label: "Ver saída", icon: <FileText />, disabled: busy, onSelect: () => onLogs(machine) },
            { label: "Publicar na internet", icon: <Globe />, disabled: busy, onSelect: () => onPublish(machine) },
            { label: "Backups", icon: <Archive />, onSelect: () => onBackups(machine) },
            { label: "Reinstalar / trocar sistema", icon: <Disc3 />, disabled: busy, onSelect: () => onReinstall(machine) },
            { label: "Remover", icon: <Trash2 />, destructive: true, disabled: busy, onSelect: () => onRemove(machine) },
          ]}
        />
      </div>
    </Card>
  );
};
