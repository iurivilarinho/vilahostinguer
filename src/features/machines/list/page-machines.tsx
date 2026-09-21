import { Plus } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, PageHeader, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { Rotas } from "@/app/variables/rotas";
import { useMachinesQuery, type MachineDto } from "../api";
import { MachinesGrid } from "../components/machines-grid";
import { MachineSheet } from "../form/machine-sheet";

const ALL_MACHINES_PARAMS = { page: 0, size: 200, sort: [{ by: "name" as const, direction: "asc" as const }] };

type DeviceGroup = {
  deviceId: number;
  deviceName: string;
  deviceHost: string;
  machines: MachineDto[];
};

export const PageMachines = () => {
  const [sheetOpen, setSheetOpen] = useState(false);
  const { data, isLoading, isError, error, refetch, isFetching } = useMachinesQuery(ALL_MACHINES_PARAMS);

  const groups = (data?.data ?? []).reduce<DeviceGroup[]>((result, machine) => {
    const group = result.find((item) => item.deviceId === machine.device.id);
    if (group) {
      group.machines.push(machine);
    } else {
      result.push({ deviceId: machine.device.id, deviceName: machine.device.name, deviceHost: machine.device.host, machines: [machine] });
    }
    return result;
  }, []);

  const newMachine = (
    <Button onClick={() => setSheetOpen(true)}>
      <Plus />
      Nova máquina
    </Button>
  );

  return (
    <>
      <PageHeader title="Máquinas" description="Máquinas Linux isoladas rodando nos seus dispositivos." actions={newMachine} />
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as máquinas.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : isLoading ? (
        <Skeleton className="h-52 w-full" />
      ) : groups.length === 0 ? (
        <MachinesGrid machines={[]} isLoading={false} deviceId={0} deviceHost="" emptyAction={newMachine} />
      ) : (
        groups.map((group) => (
          <section key={group.deviceId} className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <Typography variant="title-md" as="h2">
                {group.deviceName}
              </Typography>
              <Link to={Rotas.devices.section(group.deviceId, "maquinas")} className="inline-link text-sm">
                Abrir no dispositivo
              </Link>
            </div>
            <MachinesGrid machines={group.machines} isLoading={false} deviceId={group.deviceId} deviceHost={group.deviceHost} />
          </section>
        ))
      )}
      <MachineSheet open={sheetOpen} onOpenChange={setSheetOpen} />
    </>
  );
};
