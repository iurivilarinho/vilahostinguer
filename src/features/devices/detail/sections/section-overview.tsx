import { Battery, Clock, Cpu, HardDrive, MemoryStick, Thermometer } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip as ChartTooltip, XAxis, YAxis } from "recharts";
import { Card, CardContent, CardHeader, Progress, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { formatBytes, formatDateTime, formatRelative, formatUptime, percent } from "@/lib/format";
import { useDeviceMetricsQuery, type DeviceDto } from "../../api";
import { useDeviceOutlet } from "../device-outlet";

const HISTORY_SIZE = 60;

type Sample = {
  time: string;
  cpu: number;
  memory: number;
};

type ResourceCardProps = {
  icon: ReactNode;
  label: string;
  value: string;
  detail?: string;
  usage?: number;
};

const ResourceCard = ({ icon, label, value, detail, usage }: ResourceCardProps) => (
  <Card className="flex flex-col gap-3 p-5">
    <div className="flex items-center gap-2 text-muted-foreground [&_svg]:size-4">
      {icon}
      <Typography variant="caption">{label}</Typography>
    </div>
    <Typography variant="title-lg" as="span">
      {value}
    </Typography>
    {usage !== undefined && <Progress value={usage} aria-label={label} />}
    {detail && <Typography variant="caption">{detail}</Typography>}
  </Card>
);

type DeviceSectionProps = {
  device: DeviceDto;
};

const LiveResources = ({ device }: DeviceSectionProps) => {
  const { data: metrics, isError, error, refetch, isFetching } = useDeviceMetricsQuery(device.id);
  const [history, setHistory] = useState<Sample[]>([]);

  useEffect(() => {
    if (!metrics) {
      return;
    }
    const sample: Sample = {
      time: new Date(metrics.collectedAt).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
      cpu: Number((metrics.cpuPercent ?? 0).toFixed(1)),
      memory: Number(percent(metrics.memoryUsedBytes, metrics.memoryTotalBytes).toFixed(1)),
    };
    setHistory((current) => [...current, sample].slice(-HISTORY_SIZE));
  }, [metrics]);

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível ler o uso de recursos.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  if (!metrics) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <Skeleton className="h-32" />
        <Skeleton className="h-32" />
        <Skeleton className="h-32" />
      </div>
    );
  }

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <ResourceCard
          icon={<Cpu />}
          label="CPU"
          value={`${(metrics.cpuPercent ?? 0).toFixed(0)}%`}
          usage={metrics.cpuPercent ?? 0}
          detail={`Carga ${metrics.load1?.toFixed(2) ?? "—"} · ${metrics.load5?.toFixed(2) ?? "—"} · ${metrics.load15?.toFixed(2) ?? "—"}`}
        />
        <ResourceCard
          icon={<MemoryStick />}
          label="Memória"
          value={formatBytes(metrics.memoryUsedBytes)}
          usage={percent(metrics.memoryUsedBytes, metrics.memoryTotalBytes)}
          detail={`de ${formatBytes(metrics.memoryTotalBytes)}${metrics.swapTotalBytes ? ` · swap ${formatBytes(metrics.swapUsedBytes)}` : ""}`}
        />
        <ResourceCard
          icon={<HardDrive />}
          label="Disco (raiz)"
          value={formatBytes(metrics.diskUsedBytes)}
          usage={percent(metrics.diskUsedBytes, metrics.diskTotalBytes)}
          detail={`de ${formatBytes(metrics.diskTotalBytes)}`}
        />
        <ResourceCard icon={<Clock />} label="Ligado há" value={formatUptime(metrics.uptimeSeconds)} detail={`${metrics.processCount ?? "—"} processos`} />
        <ResourceCard
          icon={<Battery />}
          label="Bateria"
          value={metrics.batteryPercent !== null ? `${metrics.batteryPercent}%` : "—"}
          usage={metrics.batteryPercent ?? undefined}
          detail={metrics.batteryStatus ?? "Sem bateria"}
        />
        <ResourceCard
          icon={<Thermometer />}
          label="Temperatura"
          value={metrics.temperatureCelsius !== null ? `${metrics.temperatureCelsius.toFixed(1)} °C` : "—"}
          detail={`Rede: ↓ ${formatBytes(metrics.networkReceivedBytes)} · ↑ ${formatBytes(metrics.networkSentBytes)}`}
        />
      </div>
      <UsageChart history={history} />
    </>
  );
};

type UsageChartProps = {
  history: Sample[];
};

const UsageChart = ({ history }: UsageChartProps) => (
  <Card>
    <CardHeader>
      <div className="flex flex-col gap-1">
        <Typography variant="title-md">Uso nos últimos minutos</Typography>
        <Typography variant="caption">Atualizado a cada 5 segundos enquanto esta tela estiver aberta.</Typography>
      </div>
    </CardHeader>
    <CardContent className="h-64">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={history} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis dataKey="time" tick={{ fontSize: 11, fill: "var(--muted-foreground)" }} minTickGap={40} />
          <YAxis domain={[0, 100]} unit="%" tick={{ fontSize: 11, fill: "var(--muted-foreground)" }} />
          <ChartTooltip
            contentStyle={{ background: "var(--popover)", border: "1px solid var(--border)", borderRadius: 8, fontSize: 12 }}
            formatter={(value, name) => [`${value ?? 0}%`, name === "cpu" ? "CPU" : "Memória"]}
          />
          <Area type="monotone" dataKey="cpu" stroke="var(--chart-1)" fill="var(--chart-1)" fillOpacity={0.15} strokeWidth={2} isAnimationActive={false} />
          <Area type="monotone" dataKey="memory" stroke="var(--chart-2)" fill="var(--chart-2)" fillOpacity={0.12} strokeWidth={2} isAnimationActive={false} />
        </AreaChart>
      </ResponsiveContainer>
    </CardContent>
  </Card>
);

type InfoRowProps = {
  label: string;
  value: string | number | null | undefined;
  mono?: boolean;
};

const InfoRow = ({ label, value, mono = false }: InfoRowProps) => (
  <div className="flex flex-col gap-0.5 py-2">
    <Typography variant="caption">{label}</Typography>
    <Typography variant={mono ? "mono" : "body-sm"} className="break-all">
      {value === null || value === undefined || value === "" ? "—" : value}
    </Typography>
  </div>
);

const SystemInfo = ({ device }: DeviceSectionProps) => (
  <Card>
    <CardHeader>
      <div className="flex flex-col gap-1">
        <Typography variant="title-md">Sistema</Typography>
        <Typography variant="caption">Lido em {formatDateTime(device.factsUpdatedAt)}</Typography>
      </div>
    </CardHeader>
    <CardContent className="grid gap-x-6 sm:grid-cols-2 xl:grid-cols-3">
      <InfoRow label="Modelo" value={device.model} />
      <InfoRow label="Nome do host" value={device.hostname} />
      <InfoRow label="Sistema" value={[device.osName, device.osVersion].filter(Boolean).join(" ")} />
      <InfoRow label="Kernel" value={device.kernelVersion} mono />
      <InfoRow label="Arquitetura" value={device.architecture} />
      <InfoRow label="Processador" value={device.cpuModel ? `${device.cpuModel} (${device.cpuCores ?? "?"} núcleos)` : null} />
      <InfoRow label="Memória" value={formatBytes(device.memoryTotalBytes)} />
      <InfoRow label="Disco da raiz" value={formatBytes(device.diskTotalBytes)} />
      <InfoRow label="Endereço MAC" value={device.macAddress} mono />
      <InfoRow label="Pacotes / serviços" value={[device.packageManager, device.initSystem].filter(Boolean).join(" · ").toLowerCase()} />
      <InfoRow label="Acesso" value={device.credential ? `${device.credential.name} (${device.credential.username}${device.rootAccess ? ", root" : ""})` : "Sem credencial"} />
      <InfoRow label="Visto" value={formatRelative(device.lastSeenAt)} />
      <InfoRow label="Adaptador deste computador" value={device.interfaceName} />
      <InfoRow label="Chave do servidor SSH" value={`${device.hostKeyType ?? ""} ${device.hostKeyFingerprint}`.trim()} mono />
    </CardContent>
  </Card>
);

export const SectionOverview = () => {
  const { device } = useDeviceOutlet();
  const live = device.status === "READY" && device.online;
  return (
    <>
      {live && <LiveResources device={device} />}
      <SystemInfo device={device} />
    </>
  );
};

