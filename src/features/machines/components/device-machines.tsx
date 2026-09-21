import { AlertTriangle, Download, Plus, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";
import { Button, Card, CardContent, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { useInstallAppMutation } from "@/features/apps/api";
import type { DeviceDto } from "@/features/devices/api";
import { openOperationViewer } from "@/features/operations";
import { useDockerStatusQuery, useMachinesQuery, useSyncMachinesMutation } from "../api";
import { MachineSheet } from "../form/machine-sheet";
import { MachinesGrid } from "./machines-grid";

type DeviceMachinesProps = {
  device: DeviceDto;
};

export const DeviceMachines = ({ device }: DeviceMachinesProps) => {
  const [sheetOpen, setSheetOpen] = useState(false);
  const { data: docker, isLoading: isCheckingDocker, isError, error, refetch, isFetching } = useDockerStatusQuery(device.id);
  const { data: machines, isLoading } = useMachinesQuery({ page: 0, size: 100, filter: { deviceId: device.id } });
  const { mutate: sync } = useSyncMachinesMutation();
  const { mutate: installDocker, isPending: isInstalling } = useInstallAppMutation({
    onSuccess: (operation) => openOperationViewer(operation.id),
  });

  useEffect(() => {
    if (docker?.running) {
      sync(device.id);
    }
  }, [docker?.running, device.id, sync]);

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível verificar o Docker.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  if (isCheckingDocker || !docker) {
    return <Skeleton className="h-40 w-full" />;
  }

  const kernelMissing = docker.missingKernelFeatures.filter(Boolean);
  const newMachine = (
    <Button onClick={() => setSheetOpen(true)} disabled={!docker.running}>
      <Plus />
      Nova máquina
    </Button>
  );

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Typography variant="body-sm" className="text-muted-foreground">
          {docker.running
            ? `Docker ${docker.version} em funcionamento. Cada máquina é um Linux completo e isolado.`
            : "As máquinas rodam sobre o Docker deste dispositivo."}
        </Typography>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => refetch()} loading={isFetching}>
            <RefreshCw />
            Verificar
          </Button>
          {newMachine}
        </div>
      </div>

      {!docker.running && (
        <Card className="border-warning">
          <CardContent className="flex flex-col gap-3">
            <div className="flex items-center gap-2">
              <AlertTriangle className="size-5 text-warning-foreground" />
              <Typography variant="title-sm">{docker.installed ? "O Docker não está respondendo" : "O Docker não está instalado"}</Typography>
            </div>
            {kernelMissing.length > 0 && (
              <Typography variant="body-sm">
                O kernel deste dispositivo não tem recursos que o Docker exige: <strong>{kernelMissing.join(", ")}</strong>. É preciso um
                kernel compilado com eles.
              </Typography>
            )}
            {docker.message && (
              <Typography variant="mono" className="text-muted-foreground">
                {docker.message}
              </Typography>
            )}
            {!docker.installed && (
              <Button className="self-start" onClick={() => installDocker({ deviceId: device.id, app: "DOCKER" })} loading={isInstalling}>
                <Download />
                Instalar Docker
              </Button>
            )}
          </CardContent>
        </Card>
      )}

      <MachinesGrid machines={machines?.data ?? []} isLoading={isLoading} deviceId={device.id} deviceHost={device.host} emptyAction={docker.running ? newMachine : undefined} />

      <MachineSheet open={sheetOpen} onOpenChange={setSheetOpen} device={device} />
    </>
  );
};
