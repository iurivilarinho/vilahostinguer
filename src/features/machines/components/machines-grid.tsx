import { Box } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Card, ConfirmDialog, EmptyState, Skeleton } from "@/components";
import { openOperationViewer } from "@/features/operations";
import { useMachineActionMutation, useMachineStatsQuery, useRemoveMachineMutation, type MachineAction, type MachineDto } from "../api";
import { MachineCard } from "./machine-card";
import { MachineLogsDialog, MachineTerminalDialog } from "./machine-dialogs";

type MachinesGridProps = {
  machines: MachineDto[];
  isLoading: boolean;
  deviceId: number;
  deviceHost: string;
  showDevice?: boolean;
  emptyAction?: ReactNode;
};

/** Grade de máquinas de um dispositivo, com uso de recursos ao vivo e as ações de cada uma. */
export const MachinesGrid = ({ machines, isLoading, deviceId, deviceHost, showDevice = false, emptyAction }: MachinesGridProps) => {
  const [terminal, setTerminal] = useState<MachineDto | null>(null);
  const [logs, setLogs] = useState<MachineDto | null>(null);
  const [removing, setRemoving] = useState<MachineDto | null>(null);
  const anyRunning = machines.some((machine) => machine.status === "RUNNING");
  const { data: stats } = useMachineStatsQuery(deviceId, { enabled: anyRunning });
  const onStarted = { onSuccess: (operation: { id: number }) => openOperationViewer(operation.id) };
  const { mutate: runAction } = useMachineActionMutation(onStarted);
  const { mutate: remove, isPending: isRemoving } = useRemoveMachineMutation(onStarted);

  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2">
        <Skeleton className="h-52" />
        <Skeleton className="h-52" />
      </div>
    );
  }

  if (machines.length === 0) {
    return (
      <Card>
        <EmptyState
          icon={<Box />}
          title="Nenhuma máquina"
          description="Crie uma máquina Linux isolada — Ubuntu, Debian, Alpine, Fedora — com usuário, sudo e SSH próprios, para testar e hospedar sem mexer no sistema do dispositivo."
          action={emptyAction}
        />
      </Card>
    );
  }

  return (
    <>
      <div className="grid gap-4 md:grid-cols-2">
        {machines.map((machine) => (
          <MachineCard
            key={machine.id}
            machine={machine}
            stats={stats?.find((item) => item.machineId === machine.id)}
            deviceHost={deviceHost}
            showDevice={showDevice}
            onTerminal={setTerminal}
            onLogs={setLogs}
            onAction={(target, action: MachineAction) => runAction({ id: target.id, action })}
            onRemove={setRemoving}
          />
        ))}
      </div>
      <MachineTerminalDialog machine={terminal} onClose={() => setTerminal(null)} />
      <MachineLogsDialog machine={logs} onClose={() => setLogs(null)} />
      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(open) => !open && setRemoving(null)}
        title={removing ? `Remover a máquina ${removing.name}?` : "Remover"}
        description="Tudo o que está dentro da máquina é apagado. As pastas compartilhadas com o dispositivo ficam intactas."
        confirmLabel="Remover"
        destructive
        loading={isRemoving}
        typeToConfirm={removing?.name}
        onConfirm={() => removing && remove(removing.id, { onSettled: () => setRemoving(null) })}
      />
    </>
  );
};
