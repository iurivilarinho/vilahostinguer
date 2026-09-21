import { Link2, MoreHorizontal, Trash2, Unlink, Unplug } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import {
  Button,
  ConfirmDialog,
  DropdownMenu,
  Progress,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Typography,
  type DropdownMenuItem,
} from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { openOperationViewer } from "@/features/operations";
import { formatBytes, percent } from "@/lib/format";
import { useDeleteVolumeMutation, useDetachVolumeMutation, useReleaseVolumeMutation, type VolumeDto } from "../api";
import { AttachSheet } from "../form/attach-sheet";
import { VolumeStatusBadge } from "./volume-status-badge";

type VolumesTableProps = {
  volumes: VolumeDto[];
  presetDeviceId?: number;
};

const isBusy = (volume: VolumeDto) => volume.status === "ATTACHING" || volume.status === "DETACHING";

export const VolumesTable = ({ volumes, presetDeviceId }: VolumesTableProps) => {
  const [attaching, setAttaching] = useState<VolumeDto | null>(null);
  const [detaching, setDetaching] = useState<VolumeDto | null>(null);
  const [releasing, setReleasing] = useState<VolumeDto | null>(null);
  const [deleting, setDeleting] = useState<VolumeDto | null>(null);
  const { mutateAsync: detach, isPending: isDetaching } = useDetachVolumeMutation({ onSuccess: (operation) => openOperationViewer(operation.id) });
  const { mutateAsync: release, isPending: isReleasing } = useReleaseVolumeMutation();
  const { mutateAsync: remove, isPending: isDeleting } = useDeleteVolumeMutation();

  const actionsFor = (volume: VolumeDto): DropdownMenuItem[] => {
    const items: DropdownMenuItem[] = [];
    if (volume.status === "AVAILABLE" || volume.status === "FAILED") {
      items.push({ label: volume.device ? "Tentar conectar de novo" : "Conectar", icon: <Link2 />, onSelect: () => setAttaching(volume) });
    }
    if (volume.device && !isBusy(volume)) {
      items.push({ label: "Desconectar", icon: <Unplug />, onSelect: () => setDetaching(volume) });
      items.push({ label: "Liberar sem o dispositivo", icon: <Unlink />, onSelect: () => setReleasing(volume) });
    }
    if (!volume.device) {
      items.push({ label: "Apagar", icon: <Trash2 />, destructive: true, onSelect: () => setDeleting(volume) });
    }
    return items;
  };

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Disco</TableHead>
            <TableHead className="w-56">Tamanho</TableHead>
            <TableHead>Onde está</TableHead>
            <TableHead>Situação</TableHead>
            <TableHead className="w-14" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {volumes.map((volume) => (
            <TableRow key={volume.id}>
              <TableCell>
                <div className="flex flex-col">
                  <Typography variant="ui-header">{volume.name}</Typography>
                  <Typography variant="caption" className="break-all">
                    {volume.filePath}
                  </Typography>
                </div>
              </TableCell>
              <TableCell>
                <div className="flex flex-col gap-1">
                  <Typography variant="body-sm">{formatBytes(volume.sizeBytes)}</Typography>
                  <Progress value={percent(volume.writtenBytes, volume.sizeBytes)} aria-label={`Espaço já gravado em ${volume.name}`} />
                  <Typography variant="caption">{formatBytes(volume.writtenBytes)} já ocupados no PC</Typography>
                </div>
              </TableCell>
              <TableCell>
                {volume.device ? (
                  <div className="flex flex-col">
                    <Link to={Rotas.devices.section(volume.device.id, "armazenamento")} className="inline-link text-sm">
                      {volume.device.name}
                    </Link>
                    <Typography variant="mono" className="text-xs">
                      {volume.mountPath}
                    </Typography>
                    {volume.machine && (
                      <Typography variant="caption">
                        Máquina {volume.machine.name}: {volume.containerPath}
                      </Typography>
                    )}
                  </div>
                ) : (
                  <Typography variant="caption">Em nenhum lugar</Typography>
                )}
              </TableCell>
              <TableCell>
                <VolumeStatusBadge volume={volume} />
              </TableCell>
              <TableCell className="text-right">
                {actionsFor(volume).length > 0 && (
                  <DropdownMenu
                    trigger={
                      <Button variant="ghost" size="icon" aria-label={`Ações de ${volume.name}`}>
                        <MoreHorizontal />
                      </Button>
                    }
                    items={actionsFor(volume)}
                  />
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <AttachSheet volume={attaching} onClose={() => setAttaching(null)} presetDeviceId={presetDeviceId} />

      <ConfirmDialog
        open={detaching !== null}
        onOpenChange={(open) => !open && setDetaching(null)}
        title={detaching ? `Desconectar ${detaching.name}` : "Desconectar"}
        description={
          detaching?.machine
            ? `A máquina ${detaching.machine.name} é recriada sem o disco (nada do sistema dela se perde) e o disco é desmontado de ${detaching.device?.name}.`
            : `O disco é desmontado de ${detaching?.mountPath ?? ""} em ${detaching?.device?.name ?? ""}. Os dados ficam no PC.`
        }
        confirmLabel="Desconectar"
        loading={isDetaching}
        onConfirm={async () => {
          if (detaching) {
            await detach(detaching.id);
            setDetaching(null);
          }
        }}
      />

      <ConfirmDialog
        open={releasing !== null}
        onOpenChange={(open) => !open && setReleasing(null)}
        title={releasing ? `Liberar ${releasing.name} sem o dispositivo` : "Liberar"}
        description="Para um dispositivo que sumiu ou não responde: o PC para de entregar o disco e ele fica livre. Se o dispositivo ainda estiver com a pasta montada, ela passa a dar erro até ele reiniciar."
        confirmLabel="Liberar"
        destructive
        loading={isReleasing}
        onConfirm={async () => {
          if (releasing) {
            await release(releasing.id);
            setReleasing(null);
          }
        }}
      />

      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={deleting ? `Apagar ${deleting.name}` : "Apagar"}
        description={`O arquivo ${deleting?.filePath ?? ""} e tudo o que foi gravado no disco são apagados deste PC. Não há como desfazer.`}
        confirmLabel="Apagar"
        destructive
        typeToConfirm={deleting?.name}
        loading={isDeleting}
        onConfirm={async () => {
          if (deleting) {
            await remove({ id: deleting.id, confirmation: deleting.name });
            setDeleting(null);
          }
        }}
      />
    </>
  );
};
