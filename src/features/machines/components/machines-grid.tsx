import { MonitorCog } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Card, ConfirmDialog, EmptyState, Skeleton } from "@/components";
import { openOperationViewer } from "@/features/operations";
import { RouteSheet, type RouteSheetPreset } from "@/features/remote-access";
import { useMachineActionMutation, useMachineStatsQuery, useRemoveMachineMutation, type MachineAction, type MachineDto } from "../api";
import { ReinstallSheet } from "../form/reinstall-sheet";
import { MachineBackupsDialog } from "./machine-backups-dialog";
import { MachineCard } from "./machine-card";
import { MachineLogsDialog, MachineTerminalDialog } from "./machine-dialogs";

/** A sugestão ao publicar é o SSH da máquina numa porta do PC. */
const publishPreset = (machine: MachineDto): RouteSheetPreset => ({
  type: "TCP",
  target: `machine:${machine.id}`,
  targetPort: "22",
  description: `SSH da ${machine.name}`,
});

type MachinesGridProps = {
  machines: MachineDto[];
  isLoading: boolean;
  emptyAction?: ReactNode;
};

/** Grade das máquinas virtuais do PC, com uso de recursos ao vivo e as ações de cada uma. */
export const MachinesGrid = ({ machines, isLoading, emptyAction }: MachinesGridProps) => {
  const [terminal, setTerminal] = useState<MachineDto | null>(null);
  const [logs, setLogs] = useState<MachineDto | null>(null);
  const [removing, setRemoving] = useState<MachineDto | null>(null);
  const [publishing, setPublishing] = useState<RouteSheetPreset | undefined>();
  const [backups, setBackups] = useState<MachineDto | null>(null);
  const [reinstalling, setReinstalling] = useState<MachineDto | null>(null);
  const anyRunning = machines.some((machine) => machine.status === "RUNNING");
  const { data: stats } = useMachineStatsQuery({ enabled: anyRunning });
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
          icon={<MonitorCog />}
          title="Nenhuma máquina"
          description="Crie uma máquina virtual Linux neste PC — Ubuntu, Debian, Rocky ou AlmaLinux — com processadores, memória e disco próprios. Ela aparece no painel como um servidor a mais, igual ao celular."
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
        description="A máquina virtual e o disco dela são apagados deste PC. Os backups já feitos continuam guardados."
        confirmLabel="Remover"
        destructive
        loading={isRemoving}
        typeToConfirm={removing?.name}
        onConfirm={() => removing && remove(removing.id, { onSettled: () => setRemoving(null) })}
      />
    </>
  );
};
