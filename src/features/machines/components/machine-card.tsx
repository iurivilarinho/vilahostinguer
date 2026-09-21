import { Archive, Disc3, ExternalLink, FileText, Globe, MonitorCog, MoreHorizontal, Play, Power, RotateCw, SquareTerminal, Trash2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Badge, Button, Card, DropdownMenu, Progress, Typography } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { cn } from "@/lib/merge-classes";
import type { MachineDto, MachineStatsDto, MachineStatus } from "../api";

const STATUS_TONE: Record<MachineStatus, "info" | "success" | "neutral" | "destructive"> = {
  CREATING: "info",
  RUNNING: "success",
  STOPPED: "neutral",
  FAILED: "destructive",
  REMOVED: "neutral",
};

const formatMemory = (memoryMb: number) => (memoryMb >= 1024 ? `${(memoryMb / 1024).toLocaleString("pt-BR", { maximumFractionDigits: 1 })} GB` : `${memoryMb} MB`);

type MachineCardProps = {
  machine: MachineDto;
  stats?: MachineStatsDto;
  onTerminal: (machine: MachineDto) => void;
  onLogs: (machine: MachineDto) => void;
  onAction: (machine: MachineDto, action: "START" | "STOP" | "RESTART") => void;
  onRemove: (machine: MachineDto) => void;
  onPublish: (machine: MachineDto) => void;
  onBackups: (machine: MachineDto) => void;
  onReinstall: (machine: MachineDto) => void;
};

export const MachineCard = ({ machine, stats, onTerminal, onLogs, onAction, onRemove, onPublish, onBackups, onReinstall }: MachineCardProps) => {
  const navigate = useNavigate();
  const running = machine.status === "RUNNING";
  const busy = machine.status === "CREATING";
  const sshCommand = `ssh ${machine.username}@${machine.ipAddress}`;

  return (
    <Card className="flex flex-col gap-4 p-5">
      <div className="flex items-start gap-3">
        <div className={cn("flex size-11 shrink-0 items-center justify-center rounded-lg", running ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground")}>
          <MonitorCog className="size-5" />
        </div>
        <div className="flex min-w-0 flex-1 flex-col">
          <Typography variant="title-sm" className="truncate">
            {machine.name}
          </Typography>
          <Typography variant="caption" className="truncate">
            {machine.distributionName} {machine.version} · {machine.ipAddress}
          </Typography>
        </div>
        <Badge tone={STATUS_TONE[machine.status]}>{machine.statusDescription}</Badge>
      </div>

      <div className="flex flex-wrap gap-2">
        <Badge tone="primary">{machine.cpuCount} {machine.cpuCount === 1 ? "processador" : "processadores"}</Badge>
        <Badge>{formatMemory(machine.memoryMb)} de memória</Badge>
        <Badge>{machine.diskGb} GB de disco em {machine.drive}</Badge>
      </div>

      {running && stats && (
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <Typography variant="caption">CPU</Typography>
            <Typography variant="caption" className="text-foreground">
              {stats.cpuPercent?.toFixed(0) ?? "—"}%
            </Typography>
          </div>
          <Progress value={stats.cpuPercent ?? 0} aria-label="CPU" />
          <div className="flex items-center justify-between">
            <Typography variant="caption">Memória</Typography>
            <Typography variant="caption" className="text-foreground">
              {stats.memoryUsage ?? "—"}
            </Typography>
          </div>
          <Progress value={stats.memoryPercent ?? 0} aria-label="Memória" />
        </div>
      )}

      <Typography variant="mono" className="truncate rounded-md bg-muted px-2 py-1 text-muted-foreground" title={sshCommand}>
        {sshCommand}
      </Typography>

      <div className="mt-auto flex items-center justify-end gap-2">
        <Button size="sm" onClick={() => onTerminal(machine)} disabled={!running || !machine.device}>
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
            {
              label: "Abrir como dispositivo",
              icon: <ExternalLink />,
              disabled: !machine.device,
              onSelect: () => machine.device && navigate(Rotas.devices.detail(machine.device.id)),
            },
            { label: "Reiniciar", icon: <RotateCw />, disabled: !running, onSelect: () => onAction(machine, "RESTART") },
            { label: "Ver registro do sistema", icon: <FileText />, disabled: !running, onSelect: () => onLogs(machine) },
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
