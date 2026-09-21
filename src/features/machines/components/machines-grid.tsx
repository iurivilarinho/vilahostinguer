import { Box } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Card, ConfirmDialog, EmptyState, Skeleton } from "@/components";
import { openOperationViewer } from "@/features/operations";
import { RouteSheet, type RouteSheetPreset } from "@/features/remote-access";
import { useMachineActionMutation, useMachineStatsQuery, useRemoveMachineMutation, type MachineAction, type MachineDto } from "../api";
import { ReinstallSheet } from "../form/reinstall-sheet";
import { MachineBackupsDialog } from "./machine-backups-dialog";
import { MachineCard } from "./machine-card";
import { MachineLogsDialog, MachineTerminalDialog } from "./machine-dialogs";

/** Com SSH, a sugestão é publicar o SSH numa porta do PC; sem SSH, um site na porta 80. */
const publishPreset = (machine: MachineDto): RouteSheetPreset =>
  machine.sshEnabled && machine.sshPort !== null
    ? { type: "TCP", target: `machine:${machine.id}`, targetPort: String(machine.sshPort), publicPort: String(machine.sshPort), description: `SSH da ${machine.name}` }
    : { type: "HTTP", target: `machine:${machine.id}`, targetPort: "80" };

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
  const [publishing, setPublishing] = useState<RouteSheetPreset | undefined>();
  const [backups, setBackups] = useState<MachineDto | null>(null);
  const [reinstalling, setReinstalling] = useState<MachineDto | null>(null);
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
            onPublish={(target) => setPublishing(publishPreset(target))}
            onBackups={setBackups}
            onReinstall={setReinstalling}
          />
        ))}
      </div>
      <MachineTerminalDialog machine={terminal} onClose={() => setTerminal(null)} />
      <MachineLogsDialog machine={logs} onClose={() => setLogs(null)} />
      <MachineBackupsDialog machine={backups} onClose={() => setBackups(null)} />
      <ReinstallSheet machine={reinstalling} onClose={() => setReinstalling(null)} />
      <RouteSheet open={publishing !== undefined} onOpenChange={(open) => !open && setPublishing(undefined)} preset={publishing} />
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
