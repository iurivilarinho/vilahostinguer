import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Badge, Button, FieldWrapper, Input, Select, Textarea, Typography } from "@/components";
import { useDevicesQuery } from "@/features/devices/api";
import { openOperationViewer } from "@/features/operations";
import { useCreateBackupMutation } from "../api";
import { backupFormSchema, DEFAULT_BACKUP_FORM_VALUES, toPathList, type BackupFormValues } from "./schema";

const SUGGESTED_PATHS = ["/etc", "/root", "/home", "/srv", "/var/www", "/opt"];
const READY_DEVICES_PARAMS = { page: 0, size: 100, filter: { active: true, status: ["READY" as const] } };

type BackupSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  deviceId?: number;
};

export const BackupSheet = ({ open, onOpenChange, deviceId }: BackupSheetProps) => {
  const { data: devices } = useDevicesQuery(READY_DEVICES_PARAMS, { enabled: open && deviceId === undefined });
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<BackupFormValues>({
    resolver: zodResolver(backupFormSchema),
    defaultValues: DEFAULT_BACKUP_FORM_VALUES,
  });
  const { mutateAsync: createBackup, isPending: isSaving } = useCreateBackupMutation({
    onSuccess: (backup) => backup.operationId && openOperationViewer(backup.operationId),
  });
  const paths = toPathList(watch("paths"));

  useEffect(() => {
    if (open) {
      const stamp = new Date().toLocaleDateString("pt-BR");
      reset({ ...DEFAULT_BACKUP_FORM_VALUES, deviceId: deviceId ? String(deviceId) : "", name: `Backup de ${stamp}` });
    }
  }, [open, deviceId, reset]);

  const togglePath = (path: string) => {
    const next = paths.includes(path) ? paths.filter((item) => item !== path) : [...paths, path];
    setValue("paths", next.join("\n"), { shouldValidate: true });
  };

  const onSubmit = async (values: BackupFormValues) => {
    await createBackup({ deviceId: Number(values.deviceId), name: values.name, paths: toPathList(values.paths) });
    onOpenChange(false);
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title="Novo backup"
      description="As pastas são compactadas no dispositivo e o arquivo .tar.gz é salvo neste computador."
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form="backup-form" loading={isSaving}>
            Gerar backup
          </Button>
        </>
      }
    >
      <form id="backup-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        {deviceId === undefined && (
          <FieldWrapper label="Dispositivo" htmlFor="backup-device" error={errors.deviceId?.message}>
            <Select id="backup-device" {...register("deviceId")} aria-invalid={Boolean(errors.deviceId)}>
              <option value="">Escolha</option>
              {devices?.data.map((device) => (
                <option key={device.id} value={String(device.id)} disabled={!device.online}>
                  {device.name}
                  {device.online ? "" : " (desconectado)"}
                </option>
              ))}
            </Select>
          </FieldWrapper>
        )}
        <FieldWrapper label="Nome" htmlFor="backup-name" error={errors.name?.message}>
          <Input id="backup-name" {...register("name")} aria-invalid={Boolean(errors.name)} />
        </FieldWrapper>
        <div className="flex flex-col gap-2">
          <Typography variant="ui-header">Sugestões</Typography>
          <div className="flex flex-wrap gap-2">
            {SUGGESTED_PATHS.map((path) => (
              <button key={path} type="button" onClick={() => togglePath(path)} className="cursor-pointer">
                <Badge tone={paths.includes(path) ? "primary" : "neutral"} className="font-mono">
                  {path}
                </Badge>
              </button>
            ))}
          </div>
        </div>
        <FieldWrapper label="Pastas" htmlFor="backup-paths" error={errors.paths?.message} description="Uma por linha. Pastas que não existirem são ignoradas.">
          <Textarea id="backup-paths" rows={6} className="font-mono text-xs" {...register("paths")} aria-invalid={Boolean(errors.paths)} />
        </FieldWrapper>
      </form>
    </AppSheet>
  );
};
