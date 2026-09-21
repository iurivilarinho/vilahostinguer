import { Plus } from "lucide-react";
import { useState } from "react";
import { Button, Typography } from "@/components";
import { BackupSheet, BackupsTable } from "@/features/backups";
import { useDeviceOutlet } from "../device-outlet";

export const SectionBackups = () => {
  const { device } = useDeviceOutlet();
  const [sheetOpen, setSheetOpen] = useState(false);
  const canBackup = device.status === "READY" && device.online;

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Typography variant="body-sm" className="text-muted-foreground">
          Backups das pastas de {device.name}. Restaurar devolve os arquivos ao mesmo dispositivo.
        </Typography>
        <Button onClick={() => setSheetOpen(true)} disabled={!canBackup}>
          <Plus />
          Novo backup
        </Button>
      </div>
      <BackupsTable filter={{ deviceId: device.id }} showDevice={false} storageKey="deviceBackupsPagination" />
      <BackupSheet open={sheetOpen} onOpenChange={setSheetOpen} deviceId={device.id} />
    </>
  );
};
