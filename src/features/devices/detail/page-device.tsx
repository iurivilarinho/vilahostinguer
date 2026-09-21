import { Activity, Archive, ArchiveRestore, Cable, DatabaseBackup, FolderOpen, HardDrive, LayoutDashboard, MonitorCog, MoreHorizontal, Network, Package, RefreshCw, Server, Settings, SquareTerminal } from "lucide-react";
import type { ReactNode } from "react";
import { Link, NavLink, Outlet, useNavigate, useParams } from "react-router-dom";
import { Badge, Button, Card, DropdownMenu, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { Rotas, type DeviceSection } from "@/app/variables/rotas";
import { cn } from "@/lib/merge-classes";
import { useChangeDeviceActiveMutation, useDeviceQuery, useRefreshDeviceFactsMutation } from "../api";
import { DeviceStatusBadge, OnlineBadge } from "../components/device-status-badge";
import type { DeviceOutletContext } from "./device-outlet";

type SectionLink = {
  section: DeviceSection;
  label: string;
  icon: ReactNode;
};

const SECTIONS: SectionLink[] = [
  { section: "visao-geral", label: "Visão geral", icon: <LayoutDashboard /> },
  { section: "terminal", label: "Terminal", icon: <SquareTerminal /> },
  { section: "aplicativos", label: "Aplicativos", icon: <Package /> },
  { section: "arquivos", label: "Arquivos", icon: <FolderOpen /> },
  { section: "backups", label: "Backups", icon: <DatabaseBackup /> },
  { section: "armazenamento", label: "Armazenamento", icon: <HardDrive /> },
  { section: "atividades", label: "Atividades", icon: <Activity /> },
  { section: "configuracoes", label: "Configurações", icon: <Settings /> },
];

export const PageDevice = () => {
  const { id } = useParams<{ id: string }>();
  const deviceId = Number(id);
  const validId = Number.isInteger(deviceId) && deviceId > 0;
  const navigate = useNavigate();
  const { data: device, isLoading, isError, error, refetch, isFetching } = useDeviceQuery(validId ? deviceId : undefined);
  const { mutate: refreshFacts, isPending: isRefreshing } = useRefreshDeviceFactsMutation();
  const { mutate: changeActive } = useChangeDeviceActiveMutation();

  if (!validId) {
    return <QueryErrorState message="Endereço de dispositivo inválido." />;
  }
  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar o dispositivo.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  if (isLoading || !device) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  const system = [device.osName, device.osVersion].filter(Boolean).join(" ");
  const context: DeviceOutletContext = { device };

  return (
    <>
      <nav aria-label="Trilha" className="flex items-center gap-2">
        <Link to={Rotas.devices.list} className="inline-link text-sm">
          Dispositivos
        </Link>
        <Typography variant="caption">/</Typography>
        <Typography variant="caption" className="text-foreground">
          {device.name}
        </Typography>
      </nav>

      <Card className="flex flex-wrap items-center gap-5 p-5">
        <div className="flex size-14 items-center justify-center rounded-xl bg-primary text-primary-foreground">
          <Server className="size-7" />
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-2">
          <div className="flex flex-wrap items-center gap-3">
            <Typography variant="title-lg" as="h1">
              {device.name}
            </Typography>
            <OnlineBadge online={device.online} />
            <DeviceStatusBadge status={device.status} label={device.statusDescription} />
            {!device.active && <Badge>Arquivado</Badge>}
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
            <Typography variant="caption">{system || "Sistema ainda não identificado"}</Typography>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="outline" onClick={() => refreshFacts(device.id)} loading={isRefreshing} disabled={!device.credential}>
            <RefreshCw />
            Atualizar informações
          </Button>
          <Button onClick={() => navigate(Rotas.devices.section(device.id, "terminal"))} disabled={device.status !== "READY" || !device.online}>
            <SquareTerminal />
            Terminal
          </Button>
          <DropdownMenu
            trigger={
              <Button variant="ghost" size="icon" aria-label="Mais ações">
                <MoreHorizontal />
              </Button>
            }
            items={[
              device.active
                ? { label: "Arquivar dispositivo", icon: <Archive />, destructive: true, onSelect: () => changeActive({ id: device.id, active: false }) }
                : { label: "Reativar dispositivo", icon: <ArchiveRestore />, onSelect: () => changeActive({ id: device.id, active: true }) },
            ]}
          />
        </div>
      </Card>

      <div className="grid gap-6 lg:grid-cols-[14rem_minmax(0,1fr)]">
        <nav aria-label="Seções do dispositivo" className="flex flex-row gap-1 overflow-x-auto lg:flex-col">
          {SECTIONS.map((item) => (
            <NavLink
              key={item.section}
              to={Rotas.devices.section(device.id, item.section)}
              className={({ isActive }) =>
                cn(
                  "nav-link flex shrink-0 items-center gap-3 rounded-lg px-3 py-2.5 text-sidebar-foreground transition-colors hover:bg-card [&_svg]:size-4",
                  isActive && "bg-card text-primary shadow-card",
                )
              }
            >
              {item.icon}
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="flex min-w-0 flex-col gap-6">
          <Outlet context={context} />
        </div>
      </div>
    </>
  );
};
