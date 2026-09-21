import { Archive, ArrowLeft, Disc3, LayoutGrid, Play, Power, RotateCw, Settings, SquareTerminal } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Button, Card, QueryErrorState, Skeleton, Tabs, Typography, buttonVariants } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { RotasPortal, type ServerSection } from "@/app/variables/rotas-portal";
import { useServerActionMutation, useServerQuery } from "../api";
import { OperationDialog } from "../components/operation-dialog";
import { ServerStatusBadge, serverState } from "../components/server-status-badge";
import { SectionBackups } from "../sections/section-backups";
import { SectionOs } from "../sections/section-os";
import { SectionOverview } from "../sections/section-overview";
import { SectionSettings } from "../sections/section-settings";
import { SectionTerminal } from "../sections/section-terminal";

const SECTIONS = [
  { value: "visao-geral" as const, label: "Visão geral", icon: <LayoutGrid /> },
  { value: "terminal" as const, label: "Terminal", icon: <SquareTerminal /> },
  { value: "sistema" as const, label: "Sistema operacional", icon: <Disc3 /> },
  { value: "backups" as const, label: "Backups", icon: <Archive /> },
  { value: "configuracoes" as const, label: "Configurações", icon: <Settings /> },
];

const isSection = (value: string | undefined): value is ServerSection => SECTIONS.some((section) => section.value === value);

export const PageServer = () => {
  const navigate = useNavigate();
  const { id, section } = useParams<{ id: string; section: string }>();
  const serverId = Number(id);
  const current: ServerSection = isSection(section) ? section : "visao-geral";
  const [operationId, setOperationId] = useState<number | null>(null);
  const { data: server, isLoading, isError, error, refetch, isFetching } = useServerQuery(Number.isFinite(serverId) ? serverId : undefined);
  const { mutate: runAction, isPending, variables } = useServerActionMutation({ onSuccess: (operation) => setOperationId(operation.id) });

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar o servidor.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  if (isLoading || !server) {
    return <Skeleton className="h-96 w-full" />;
  }

  const state = serverState(server);
  const active = server.subscriptionStatus === "ACTIVE";
  const busy = server.machineStatus === "CREATING";

  return (
    <>
      <Link to={RotasPortal.servers} className="inline-link flex items-center gap-1 text-sm">
        <ArrowLeft className="size-4" />
        VPS
      </Link>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <div className="flex flex-wrap items-center gap-3">
            <Typography variant="display-sm">{server.hostname}</Typography>
            <ServerStatusBadge server={server} />
          </div>
          <Typography variant="body-sm" className="text-muted-foreground">
            {server.planName} · {server.distributionName} {server.version}
          </Typography>
        </div>
        {active && (
          <div className="flex flex-wrap gap-2">
            {state.running ? (
              <>
                <Button variant="outline" loading={isPending && variables?.action === "RESTART"} disabled={busy} onClick={() => runAction({ id: server.id, action: "RESTART" })}>
                  <RotateCw />
                  Reiniciar
                </Button>
                <Button variant="outline" loading={isPending && variables?.action === "STOP"} disabled={busy} onClick={() => runAction({ id: server.id, action: "STOP" })}>
                  <Power />
                  Desligar
                </Button>
              </>
            ) : (
              <Button loading={isPending && variables?.action === "START"} disabled={busy} onClick={() => runAction({ id: server.id, action: "START" })}>
                <Play />
                Ligar
              </Button>
            )}
          </div>
        )}
      </div>

      {server.notice && (
        <Card className="flex flex-wrap items-center justify-between gap-3 border-info bg-info-soft p-4">
          <Typography variant="body-sm" className="text-info-foreground">
            {server.notice}
          </Typography>
          {(server.subscriptionStatus === "PENDING_PAYMENT" || server.subscriptionStatus === "SUSPENDED") && (
            <Link to={RotasPortal.billing} className={buttonVariants({ size: "sm" })}>
              Ver faturas
            </Link>
          )}
        </Card>
      )}

      <Tabs value={current} onValueChange={(value) => navigate(RotasPortal.serverSection(server.id, value))} items={SECTIONS} />

      {current === "visao-geral" && <SectionOverview server={server} />}
      {current === "terminal" && <SectionTerminal server={server} />}
      {current === "sistema" && <SectionOs server={server} onOperation={setOperationId} />}
      {current === "backups" && <SectionBackups server={server} onOperation={setOperationId} />}
      {current === "configuracoes" && <SectionSettings server={server} />}

      <OperationDialog serverId={server.id} operationId={operationId} onClose={() => setOperationId(null)} />
    </>
  );
};
