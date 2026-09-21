import { RefreshCw } from "lucide-react";
import { Button, Dialog, LogViewer, Skeleton } from "@/components";
import { TerminalView } from "@/features/terminal";
import { useMachineLogsQuery, type MachineDto } from "../api";

type MachineDialogProps = {
  machine: MachineDto | null;
  onClose: () => void;
};

/** Terminal como root na máquina (SSH). Fechar encerra o shell. */
export const MachineTerminalDialog = ({ machine, onClose }: MachineDialogProps) => (
  <Dialog
    open={machine !== null}
    onOpenChange={(open) => !open && onClose()}
    title={machine ? `Terminal — ${machine.name}` : "Terminal"}
    description={machine ? `${machine.distributionName} ${machine.version} em ${machine.ipAddress}` : undefined}
    className="max-w-5xl"
  >
    {machine && <TerminalView machineId={machine.id} className="h-[60vh]" />}
  </Dialog>
);

export const MachineLogsDialog = ({ machine, onClose }: MachineDialogProps) => {
  const { data, isLoading, refetch, isFetching } = useMachineLogsQuery(machine?.id);
  return (
    <Dialog
      open={machine !== null}
      onOpenChange={(open) => !open && onClose()}
      title={machine ? `Saída de ${machine.name}` : "Saída"}
      description="Últimas linhas do registro do sistema (journalctl)."
      className="max-w-3xl"
      footer={
        <>
          <Button variant="outline" onClick={() => refetch()} loading={isFetching}>
            <RefreshCw />
            Atualizar
          </Button>
          <Button variant="outline" onClick={onClose}>
            Fechar
          </Button>
        </>
      }
    >
      {isLoading ? <Skeleton className="h-64 w-full" /> : <LogViewer text={data?.log || "(sem saída)"} />}
    </Dialog>
  );
};
