import { Copy, MoreHorizontal, Pause, Pencil, Play, Plus, Route as RouteIcon, Trash2 } from "lucide-react";
import { useState } from "react";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DropdownMenu,
  EmptyState,
  QueryErrorState,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Typography,
  notify,
} from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { formatBytes, formatRelative } from "@/lib/format";
import { useChangeRouteStatusMutation, useRoutesQuery, type GatewayStatusDto, type RouteDto, type RouteStatus } from "../api";
import { RouteSheet } from "../form/route-sheet";
import { routeAddress, routeTargetLabel } from "../utils/route-address";

const STATUS_TONE: Record<RouteStatus, "success" | "neutral" | "destructive"> = {
  ACTIVE: "success",
  PAUSED: "neutral",
  REMOVED: "destructive",
};

type RoutesPanelProps = {
  gateway?: GatewayStatusDto;
};

/** Rotas de acesso remoto, com o endereço de fora, o destino e o tráfego desde que o painel abriu. */
export const RoutesPanel = ({ gateway }: RoutesPanelProps) => {
  const [sheetOpen, setSheetOpen] = useState(false);
  const [editing, setEditing] = useState<RouteDto | undefined>();
  const [removing, setRemoving] = useState<RouteDto | null>(null);
  const { data, isLoading, isError, error, refetch, isFetching } = useRoutesQuery({ page: 0, size: 200, sort: [{ by: "type", direction: "asc" }] });
  const { mutate: changeStatus, isPending: isChanging } = useChangeRouteStatusMutation();
  const routes = data?.data ?? [];

  const openCreate = () => {
    setEditing(undefined);
    setSheetOpen(true);
  };

  const copy = async (text: string) => {
    await navigator.clipboard.writeText(text);
    notify.success("Endereço copiado", text);
  };

  const newRoute = (
    <Button onClick={openCreate}>
      <Plus />
      Nova rota
    </Button>
  );

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-end">{newRoute}</div>
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as rotas.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : (
        <Card>
          {isLoading ? (
            <div className="flex flex-col gap-3 p-5">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
            </div>
          ) : routes.length === 0 ? (
            <EmptyState
              icon={<RouteIcon />}
              title="Nenhuma rota ainda"
              description="Uma rota publica um serviço: um site de uma máquina num nome do seu domínio, ou o SSH dela numa porta do PC."
              action={newRoute}
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Endereço de fora</TableHead>
                  <TableHead>Destino</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Tráfego</TableHead>
                  <TableHead>Situação</TableHead>
                  <TableHead className="w-12" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {routes.map((route) => {
                  const address = routeAddress(route, gateway);
                  const traffic = gateway?.traffic.find((item) => item.routeId === route.id);
                  return (
                    <TableRow key={route.id}>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <Typography variant="mono" className="truncate" title={address}>
                            {address}
                          </Typography>
                          <Button variant="ghost" size="icon" onClick={() => copy(address)} aria-label={`Copiar ${address}`}>
                            <Copy />
                          </Button>
                        </div>
                        {route.description && <Typography variant="caption">{route.description}</Typography>}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{routeTargetLabel(route)}</Typography>
                        {route.machine?.status === "REMOVED" && <Badge tone="destructive">Máquina removida</Badge>}
                      </TableCell>
                      <TableCell>
                        <Badge tone="primary">{route.typeDescription}</Badge>
                      </TableCell>
                      <TableCell>
                        {traffic ? (
                          <div className="flex flex-col">
                            <Typography variant="body-sm">
                              {traffic.totalConnections} conexões · ↓{formatBytes(traffic.bytesIn)} ↑{formatBytes(traffic.bytesOut)}
                            </Typography>
                            {traffic.lastError ? (
                              <Typography variant="caption" className="text-destructive-foreground" title={traffic.lastError}>
                                Falhou {formatRelative(traffic.lastErrorAt)}
                              </Typography>
                            ) : (
                              <Typography variant="caption">Última {formatRelative(traffic.lastConnectionAt)}</Typography>
                            )}
                          </div>
                        ) : (
                          <Typography variant="caption">Sem conexões</Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge tone={STATUS_TONE[route.status]}>{route.statusDescription}</Badge>
                      </TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={
                            <Button variant="ghost" size="icon" aria-label={`Ações da rota ${address}`}>
                              <MoreHorizontal />
                            </Button>
                          }
                          items={[
                            {
                              label: "Editar",
                              icon: <Pencil />,
                              onSelect: () => {
                                setEditing(route);
                                setSheetOpen(true);
                              },
                            },
                            route.status === "ACTIVE"
                              ? { label: "Pausar", icon: <Pause />, onSelect: () => changeStatus({ id: route.id, status: "PAUSED" }) }
                              : { label: "Reativar", icon: <Play />, onSelect: () => changeStatus({ id: route.id, status: "ACTIVE" }) },
                            { label: "Remover", icon: <Trash2 />, destructive: true, onSelect: () => setRemoving(route) },
                          ]}
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </Card>
      )}
      <RouteSheet open={sheetOpen} onOpenChange={setSheetOpen} route={editing} />
      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(open) => !open && setRemoving(null)}
        title="Remover a rota?"
        description={removing ? `${routeAddress(removing, gateway)} deixa de responder na hora. A máquina e o serviço continuam como estão.` : ""}
        confirmLabel="Remover"
        destructive
        loading={isChanging}
        onConfirm={() => removing && changeStatus({ id: removing.id, status: "REMOVED" }, { onSettled: () => setRemoving(null) })}
      />
    </div>
  );
};
