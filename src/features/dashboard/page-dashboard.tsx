import { AlertTriangle, Archive, Loader, Server } from "lucide-react";
import { Link } from "react-router-dom";
import { PageHeader, Skeleton, StatCard, Typography } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { useCredentialsQuery } from "@/features/credentials/api";
import { DeviceCard } from "@/features/devices";
import { useDevicesQuery } from "@/features/devices/api";
import { OperationsTable } from "@/features/operations";
import { formatRelative } from "@/lib/format";
import { useDashboardSummaryQuery } from "./api";
import { GettingStarted } from "./components/getting-started";

const HIGHLIGHT_PARAMS = {
  page: 0,
  size: 6,
  filter: { active: true },
  sort: [
    { by: "online" as const, direction: "desc" as const },
    { by: "name" as const, direction: "asc" as const },
  ],
};
const DEFAULT_CREDENTIAL_PARAMS = { page: 0, size: 50, filter: { active: true } };

export const PageDashboard = () => {
  const { data: summary } = useDashboardSummaryQuery();
  const { data: devices, isLoading } = useDevicesQuery(HIGHLIGHT_PARAMS, { refetchInterval: 15_000 });
  const { data: credentials } = useCredentialsQuery(DEFAULT_CREDENTIAL_PARAMS);

  const deviceList = devices?.data ?? [];

  return (
    <>
      <PageHeader title="Início" description="Seus servidores caseiros em um lugar só." />

      <GettingStarted
        hasDefaultCredential={Boolean(credentials?.data.some((credential) => credential.defaultCredential))}
        hasDevices={(summary?.totalDevices ?? 0) > 0}
        hasReadyDevice={deviceList.some((device) => device.status === "READY")}
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Dispositivos online"
          value={summary ? `${summary.onlineDevices} de ${summary.totalDevices}` : "—"}
          icon={<Server />}
          tone="success"
        />
        <StatCard
          label="Precisam de atenção"
          value={summary?.devicesNeedingAttention ?? "—"}
          icon={<AlertTriangle />}
          tone={summary && summary.devicesNeedingAttention > 0 ? "warning" : "primary"}
          hint="Sem credencial ou com acesso recusado"
        />
        <StatCard
          label="Operações em andamento"
          value={summary?.runningOperations ?? "—"}
          icon={<Loader />}
          tone="info"
          hint={summary && summary.failedOperationsLastDay > 0 ? `${summary.failedOperationsLastDay} falha(s) nas últimas 24 h` : "Nenhuma falha nas últimas 24 h"}
        />
        <StatCard
          label="Backups disponíveis"
          value={summary?.availableBackups ?? "—"}
          icon={<Archive />}
          hint={`Último ${formatRelative(summary?.lastBackupAt)}`}
        />
      </div>

      <section className="flex flex-col gap-4">
        <div className="flex items-center justify-between gap-3">
          <Typography variant="title-md" as="h2">
            Seus dispositivos
          </Typography>
          <Link to={Rotas.devices.list} className="inline-link text-sm">
            Ver todos
          </Link>
        </div>
        {isLoading ? (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <Skeleton className="h-56" />
            <Skeleton className="h-56" />
          </div>
        ) : deviceList.length === 0 ? (
          <Typography variant="body-sm" className="text-muted-foreground">
            Nenhum dispositivo ainda. Conecte um aparelho pelo cabo USB.
          </Typography>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {deviceList.map((device) => (
              <DeviceCard key={device.id} device={device} />
            ))}
          </div>
        )}
      </section>

      <section className="flex flex-col gap-4">
        <div className="flex items-center justify-between gap-3">
          <Typography variant="title-md" as="h2">
            Atividades recentes
          </Typography>
          <Link to={Rotas.operations} className="inline-link text-sm">
            Ver histórico
          </Link>
        </div>
        <OperationsTable filter={{}} storageKey="dashboardOperationsPagination" />
      </section>
    </>
  );
};
