import { Archive } from "lucide-react";
import { useState } from "react";
import { Button, Dialog, FieldWrapper, Input, Typography } from "@/components";
import { BackupsTable } from "@/features/backups";
import { openOperationViewer } from "@/features/operations";
import { useBackupMachineMutation, type MachineDto } from "../api";

type MachineBackupsDialogProps = {
  machine: MachineDto | null;
  onClose: () => void;
};

/** Backups da máquina inteira: gerar agora, restaurar, baixar ou descartar. */
export const MachineBackupsDialog = ({ machine, onClose }: MachineBackupsDialogProps) => {
  const [name, setName] = useState("");
  const { mutate: backup, isPending } = useBackupMachineMutation({
    onSuccess: (created) => {
      setName("");
      if (created.operationId !== null) {
        openOperationViewer(created.operationId);
      }
    },
  });
  const busy = machine?.status === "CREATING" || machine?.status === "REMOVED";

  return (
    <Dialog
      open={machine !== null}
      onOpenChange={(open) => !open && onClose()}
      title={machine ? `Backups de ${machine.name}` : "Backups"}
      description="Guarda a máquina inteira — sistema, programas e arquivos — neste computador. As pastas compartilhadas ficam de fora (use o backup de pastas do dispositivo)."
      className="max-w-4xl"
    >
      {machine && (
        <div className="flex flex-col gap-4">
          <form
            className="flex flex-wrap items-end gap-3"
            onSubmit={(event) => {
              event.preventDefault();
              backup({ id: machine.id, name });
            }}
          >
            <FieldWrapper label="Nome do backup" htmlFor="machine-backup-name" className="min-w-64 flex-1">
              <Input id="machine-backup-name" value={name} maxLength={120} placeholder={`Máquina ${machine.name}`} onChange={(event) => setName(event.target.value)} />
            </FieldWrapper>
            <Button type="submit" loading={isPending} disabled={busy}>
              <Archive />
              Fazer backup agora
            </Button>
          </form>
          <Typography variant="caption" as="p">
            A máquina fica pausada durante a cópia, para os arquivos saírem consistentes (um banco de dados copiado rodando pode não abrir).
          </Typography>
          <BackupsTable filter={{ machineId: machine.id, kind: ["MACHINE"] }} showDevice={false} storageKey={`machineBackups-${machine.id}`} />
        </div>
      )}
    </Dialog>
  );
};
