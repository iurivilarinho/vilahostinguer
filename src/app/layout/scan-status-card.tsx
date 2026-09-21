import { Radar, Usb } from "lucide-react";
import { Button, Typography } from "@/components";
import { useScanNowMutation, useScanStatusQuery } from "@/features/devices/api";
import { formatRelative } from "@/lib/format";

/** Rodapé do menu: mostra que a detecção automática está viva e quais cabos USB foram vistos. */
export const ScanStatusCard = () => {
  const { data: status } = useScanStatusQuery();
  const { mutate: scanNow, isPending } = useScanNowMutation();
  const usbCount = status?.usbInterfaces.length ?? 0;

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-sidebar-border bg-muted/50 p-3">
      <div className="flex items-center gap-2">
        <Radar className={status?.enabled ? "size-4 animate-pulse text-primary" : "size-4 text-muted-foreground"} />
        <Typography variant="ui-header">{status?.enabled ? "Procurando dispositivos" : "Busca automática desligada"}</Typography>
      </div>
      <div className="flex items-center gap-2">
        <Usb className="size-4 text-muted-foreground" />
        <Typography variant="caption">
          {usbCount === 0 ? "Nenhum cabo USB de rede" : `${usbCount} adaptador(es) USB`}
        </Typography>
      </div>
      <Typography variant="caption">Última busca {formatRelative(status?.lastScanAt)}</Typography>
      <Button variant="outline" size="sm" onClick={() => scanNow()} loading={isPending}>
        Procurar agora
      </Button>
    </div>
  );
};
