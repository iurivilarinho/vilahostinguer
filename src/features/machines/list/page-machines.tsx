import { CheckCircle2, Cpu, MemoryStick, Network, Plus, TriangleAlert } from "lucide-react";
import { useState } from "react";
import { Button, Card, PageHeader, Progress, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { percent } from "@/lib/format";
import { useMachineHostQuery, useMachinesQuery, type MachineHostDto } from "../api";
import { MachinesGrid } from "../components/machines-grid";
import { MachineSheet } from "../form/machine-sheet";

const ALL_MACHINES_PARAMS = { page: 0, size: 200, sort: [{ by: "name" as const, direction: "asc" as const }] };

const formatMemory = (memoryMb: number) => `${(memoryMb / 1024).toLocaleString("pt-BR", { maximumFractionDigits: 1 })} GB`;

/** Este PC como anfitrião: o Hyper-V está pronto? quanta memória ainda cabe em máquinas? */
const HostPanel = ({ host }: { host: MachineHostDto }) => {
  if (!host.ready) {
    return (
      <div className="flex gap-3 rounded-lg border border-warning bg-warning-soft p-4 text-warning-foreground">
        <TriangleAlert className="size-5 shrink-0" />
        <div className="flex flex-col gap-1">
          <Typography variant="ui-header" className="text-warning-foreground">
            {host.message ?? "O Hyper-V não está pronto neste PC."}
          </Typography>
          <Typography variant="body-sm" className="text-warning-foreground">
            Abra um PowerShell como administrador e rode <code className="font-mono">tools\habilitar-hyperv.ps1 -Usuario "DOMINIO\seu.usuario"</code>. Na
            primeira vez ele ativa o Hyper-V e pede para reiniciar; rode de novo depois para criar a rede das máquinas ({host.network}).
          </Typography>
        </div>
      </div>
    );
  }
  const given = host.usedMemoryMb;
  const forMachines = host.memoryMb - host.reservedMemoryMb;
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <Card className="flex items-center gap-3 p-4">
        <CheckCircle2 className="size-5 shrink-0 text-success" />
        <div className="flex flex-col">
          <Typography variant="ui-header">Hyper-V pronto</Typography>
          <Typography variant="caption">
            {host.machineCount} {host.machineCount === 1 ? "máquina" : "máquinas"} · rede {host.network}
          </Typography>
        </div>
      </Card>
      <Card className="flex flex-col gap-2 p-4">
        <div className="flex items-center justify-between">
          <Typography variant="ui-header" className="flex items-center gap-2">
            <MemoryStick className="size-4" /> Memória para máquinas
          </Typography>
          <Typography variant="caption">
            {formatMemory(given)} de {formatMemory(forMachines)}
          </Typography>
        </div>
        <Progress value={percent(given, forMachines)} aria-label="Memória dada às máquinas" />
        <Typography variant="caption">
          {formatMemory(host.reservedMemoryMb)} ficam para o Windows · sobram {formatMemory(host.availableMemoryMb)}
        </Typography>
      </Card>
      <Card className="flex items-center gap-3 p-4">
        <Cpu className="size-5 shrink-0 text-primary" />
        <div className="flex flex-col">
          <Typography variant="ui-header">{host.cpus} processadores lógicos</Typography>
          <Typography variant="caption" className="flex items-center gap-1">
            <Network className="size-3" /> cada máquina com IP fixo e internet pelo PC
          </Typography>
        </div>
      </Card>
    </div>
  );
};

export const PageMachines = () => {
  const [sheetOpen, setSheetOpen] = useState(false);
  const host = useMachineHostQuery();
  const { data, isLoading, isError, error, refetch, isFetching } = useMachinesQuery(ALL_MACHINES_PARAMS);

  const newMachine = (
    <Button onClick={() => setSheetOpen(true)} disabled={!host.data?.ready}>
      <Plus />
      Nova máquina
    </Button>
  );

  return (
    <>
      <PageHeader
        title="Máquinas"
        description="Máquinas virtuais Linux rodando neste PC. Cada uma aparece também em Dispositivos, ao lado do celular e das placas."
        actions={newMachine}
      />
      {host.isLoading ? <Skeleton className="h-20 w-full" /> : host.data ? <HostPanel host={host.data} /> : null}
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as máquinas.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : (
        <MachinesGrid machines={data?.data ?? []} isLoading={isLoading} emptyAction={host.data?.ready ? newMachine : undefined} />
      )}
      <MachineSheet open={sheetOpen} onOpenChange={setSheetOpen} />
    </>
  );
};
