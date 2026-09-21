import { Download, MoreHorizontal, Package, Play, RefreshCw, RotateCw, Square, Trash2 } from "lucide-react";
import { useState } from "react";
import { Badge, Button, Card, ConfirmDialog, DropdownMenu, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { useUpgradeDeviceMutation } from "@/features/devices/api";
import { openOperationViewer } from "@/features/operations";
import { useAppServiceActionMutation, useDeviceAppsQuery, useInstallAppMutation, useRemoveAppMutation, type AppCategory, type DeviceAppDto } from "../api";

const CATEGORY_ORDER: AppCategory[] = ["WEB_SERVER", "RUNTIME", "DATABASE", "CONTAINER", "TOOL"];

type DeviceAppsProps = {
  deviceId: number;
};

export const DeviceApps = ({ deviceId }: DeviceAppsProps) => {
  const { data: apps, isLoading, isError, error, refetch, isFetching } = useDeviceAppsQuery(deviceId);
  const [removing, setRemoving] = useState<DeviceAppDto | null>(null);
  const [upgradeOpen, setUpgradeOpen] = useState(false);
  const onStarted = { onSuccess: (operation: { id: number }) => openOperationViewer(operation.id) };
  const { mutate: install, isPending: isInstalling, variables: installing } = useInstallAppMutation(onStarted);
  const { mutate: remove, isPending: isRemoving } = useRemoveAppMutation(onStarted);
  const { mutate: serviceAction } = useAppServiceActionMutation(onStarted);
  const { mutate: upgrade, isPending: isUpgrading } = useUpgradeDeviceMutation(onStarted);

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível ler os aplicativos do dispositivo.")} onRetry={() => refetch()} retrying={isFetching} />;
  }

  if (isLoading || !apps) {
    return (
      <div className="grid gap-4 md:grid-cols-2">
        <Skeleton className="h-32" />
        <Skeleton className="h-32" />
        <Skeleton className="h-32" />
        <Skeleton className="h-32" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Typography variant="body-sm" className="text-muted-foreground">
          Instalação pelo gerenciador de pacotes do próprio sistema. Serviços são ativados no boot e iniciados.
        </Typography>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => refetch()} loading={isFetching}>
            <RefreshCw />
            Reler
          </Button>
          <Button variant="secondary" onClick={() => setUpgradeOpen(true)}>
            <Download />
            Atualizar o sistema
          </Button>
        </div>
      </div>

      {CATEGORY_ORDER.map((category) => {
        const items = apps.filter((app) => app.category === category);
        if (items.length === 0) {
          return null;
        }
        return (
          <section key={category} className="flex flex-col gap-3">
            <Typography variant="section-label">{items[0].categoryDescription}</Typography>
            <div className="grid gap-4 md:grid-cols-2">
              {items.map((app) => (
                <Card key={app.key} className="flex flex-col gap-3 p-5">
                  <div className="flex items-start gap-3">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary">
                      <Package className="size-5" />
                    </div>
                    <div className="flex min-w-0 flex-1 flex-col">
                      <Typography variant="title-sm">{app.name}</Typography>
                      <Typography variant="caption" as="p">
                        {app.description}
                      </Typography>
                    </div>
                    {!app.available ? (
                      <Badge>Indisponível</Badge>
                    ) : app.installed ? (
                      <Badge tone="success">Instalado</Badge>
                    ) : (
                      <Badge>Não instalado</Badge>
                    )}
                  </div>

                  {app.installed && app.version && (
                    <Typography variant="mono" className="truncate text-muted-foreground" title={app.version}>
                      {app.version}
                    </Typography>
                  )}

                  {app.installed && app.serviceName && (
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge tone={app.serviceRunning ? "success" : "warning"}>
                        Serviço {app.serviceName}: {app.serviceRunning ? "rodando" : "parado"}
                      </Badge>
                      <Badge tone={app.serviceEnabled ? "primary" : "neutral"}>{app.serviceEnabled ? "Inicia no boot" : "Não inicia no boot"}</Badge>
                    </div>
                  )}

                  <div className="mt-auto flex justify-end gap-2 pt-1">
                    {app.available && !app.installed && (
                      <Button size="sm" onClick={() => install({ deviceId, app: app.key })} loading={isInstalling && installing?.app === app.key}>
                        <Download />
                        Instalar
                      </Button>
                    )}
                    {app.installed && (
                      <DropdownMenu
                        trigger={
                          <Button variant="outline" size="sm" aria-label={`Ações de ${app.name}`}>
                            <MoreHorizontal />
                            Gerenciar
                          </Button>
                        }
                        items={[
                          ...(app.serviceName
                            ? [
                                { label: "Iniciar", icon: <Play />, onSelect: () => serviceAction({ deviceId, app: app.key, action: "START" }) },
                                { label: "Parar", icon: <Square />, onSelect: () => serviceAction({ deviceId, app: app.key, action: "STOP" }) },
                                { label: "Reiniciar", icon: <RotateCw />, onSelect: () => serviceAction({ deviceId, app: app.key, action: "RESTART" }) },
                                app.serviceEnabled
                                  ? { label: "Não iniciar no boot", onSelect: () => serviceAction({ deviceId, app: app.key, action: "DISABLE" }) }
                                  : { label: "Iniciar no boot", onSelect: () => serviceAction({ deviceId, app: app.key, action: "ENABLE" }) },
                              ]
                            : []),
                          { label: "Remover", icon: <Trash2 />, destructive: true, onSelect: () => setRemoving(app) },
                        ]}
                      />
                    )}
                  </div>
                </Card>
              ))}
            </div>
          </section>
        );
      })}

      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(open) => !open && setRemoving(null)}
        title={removing ? `Remover ${removing.name}?` : "Remover"}
        description="O serviço é parado e os pacotes são removidos. Arquivos de configuração podem ficar no sistema."
        confirmLabel="Remover"
        destructive
        loading={isRemoving}
        onConfirm={() => {
          if (removing) {
            remove({ deviceId, app: removing.key }, { onSettled: () => setRemoving(null) });
          }
        }}
      />
      <ConfirmDialog
        open={upgradeOpen}
        onOpenChange={setUpgradeOpen}
        title="Atualizar os pacotes do sistema?"
        description="Baixa e instala as atualizações disponíveis. Pode levar vários minutos e usar bastante da internet."
        confirmLabel="Atualizar"
        loading={isUpgrading}
        onConfirm={() => upgrade(deviceId, { onSettled: () => setUpgradeOpen(false) })}
      />
    </div>
  );
};
