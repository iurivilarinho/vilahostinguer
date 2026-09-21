import { zodResolver } from "@hookform/resolvers/zod";
import { Info } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Button, FieldWrapper, Input, Select, Typography } from "@/components";
import { useDevicesQuery } from "@/features/devices/api";
import { useMachinesQuery } from "@/features/machines/api";
import { openOperationViewer } from "@/features/operations";
import { formatBytes } from "@/lib/format";
import { useAttachVolumeMutation, type VolumeDto } from "../api";
import { attachFormSchema, DEFAULT_ATTACH_FORM_VALUES, type AttachFormValues } from "./attach-schema";

const READY_DEVICES_PARAMS = { page: 0, size: 100, filter: { active: true, status: ["READY" as const] } };
const USABLE_MACHINES_PARAMS = { page: 0, size: 200, filter: { status: ["RUNNING" as const] } };

type AttachSheetProps = {
  volume: VolumeDto | null;
  onClose: () => void;
  /** Abre já apontando para este dispositivo (aba Armazenamento do dispositivo). */
  presetDeviceId?: number;
};

/** Entrega o disco a um dispositivo (montado numa pasta) ou a uma máquina virtual (montado dentro dela). */
export const AttachSheet = ({ volume, onClose, presetDeviceId }: AttachSheetProps) => {
  const open = volume !== null;
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<AttachFormValues>({
    resolver: zodResolver(attachFormSchema),
    defaultValues: DEFAULT_ATTACH_FORM_VALUES,
  });
  const { data: devices } = useDevicesQuery(READY_DEVICES_PARAMS, { enabled: open });
  const { data: machines } = useMachinesQuery(USABLE_MACHINES_PARAMS, { enabled: open });
  const { mutateAsync: attach, isPending } = useAttachVolumeMutation({ onSuccess: (operation) => openOperationViewer(operation.id) });
  const target = watch("target");

  useEffect(() => {
    if (volume) {
      const deviceId = volume.device?.id ?? presetDeviceId;
      reset({
        ...DEFAULT_ATTACH_FORM_VALUES,
        deviceId: deviceId ? String(deviceId) : "",
        mountPath: volume.mountPath ?? "",
      });
    }
  }, [volume, presetDeviceId, reset]);

  const onSubmit = async (values: AttachFormValues) => {
    if (!volume) {
      return;
    }
    await attach(
      values.target === "MACHINE"
        ? { id: volume.id, machineId: Number(values.machineId), mountPath: values.mountPath || undefined, containerPath: values.containerPath }
        : { id: volume.id, deviceId: Number(values.deviceId), mountPath: values.mountPath || undefined },
    );
    onClose();
  };

  const defaultMount = volume ? `/mnt/${volume.name}` : "";

  return (
    <AppSheet
      open={open}
      onOpenChange={(next) => !next && onClose()}
      title={volume ? `Conectar ${volume.name}` : "Conectar disco"}
      description={volume ? `${formatBytes(volume.sizeBytes)} guardados em ${volume.drive}` : undefined}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={isPending}>
            Cancelar
          </Button>
          <Button type="submit" form="attach-form" loading={isPending}>
            Conectar
          </Button>
        </>
      }
    >
      <form id="attach-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <FieldWrapper label="Para quem" htmlFor="attach-target">
          <Select id="attach-target" {...register("target")}>
            <option value="DEVICE">O próprio dispositivo (uma pasta dele)</option>
            <option value="MACHINE">Uma máquina virtual deste PC (montado dentro dela)</option>
          </Select>
        </FieldWrapper>

        {target === "DEVICE" ? (
          <FieldWrapper label="Dispositivo" htmlFor="attach-device" error={errors.deviceId?.message}>
            <Select id="attach-device" {...register("deviceId")} aria-invalid={Boolean(errors.deviceId)}>
              <option value="">Escolha…</option>
              {(devices?.data ?? []).map((device) => (
                <option key={device.id} value={device.id}>
                  {device.name} ({device.host})
                </option>
              ))}
            </Select>
          </FieldWrapper>
        ) : (
          <>
            <FieldWrapper label="Máquina" htmlFor="attach-machine" error={errors.machineId?.message}>
              <Select id="attach-machine" {...register("machineId")} aria-invalid={Boolean(errors.machineId)}>
                <option value="">Escolha…</option>
                {(machines?.data ?? []).map((machine) => (
                  <option key={machine.id} value={machine.id}>
                    {machine.name} ({machine.ipAddress})
                  </option>
                ))}
              </Select>
            </FieldWrapper>
            <FieldWrapper
              label="Pasta dentro da máquina"
              htmlFor="attach-container-path"
              error={errors.containerPath?.message}
              description="Por exemplo /dados, /var/www ou /var/lib/mysql."
            >
              <Input id="attach-container-path" autoComplete="off" {...register("containerPath")} aria-invalid={Boolean(errors.containerPath)} />
            </FieldWrapper>
          </>
        )}

        {target === "DEVICE" && (
          <FieldWrapper
            label="Pasta de montagem no dispositivo"
            htmlFor="attach-mount-path"
            error={errors.mountPath?.message}
            description={`Vazio: ${defaultMount}. Precisa ficar dentro de /mnt, /media, /srv, /home, /opt ou /data e estar vazia.`}
          >
            <Input id="attach-mount-path" placeholder={defaultMount} autoComplete="off" {...register("mountPath")} aria-invalid={Boolean(errors.mountPath)} />
          </FieldWrapper>
        )}

        <div className="flex gap-3 rounded-lg border border-border bg-muted/40 p-3">
          <Info className="size-5 shrink-0 text-primary" />
          <div className="flex flex-col gap-1">
            {!volume?.formatted && (
              <Typography variant="caption" as="p">
                Na primeira conexão o disco é formatado em ext4 (ele está vazio). Depois disso nunca mais.
              </Typography>
            )}
            {target === "MACHINE" && (
              <Typography variant="caption" as="p">
                A máquina precisa estar ligada. O disco é montado dentro dela e passa a ser do usuário dela.
              </Typography>
            )}
            <Typography variant="caption" as="p">
              O dispositivo precisa de suporte a NBD no kernel e alcançar este PC na porta dos discos; o painel instala o nbd-client se faltar.
            </Typography>
          </div>
        </div>
      </form>
    </AppSheet>
  );
};
