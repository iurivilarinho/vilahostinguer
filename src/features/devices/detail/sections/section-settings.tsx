import { zodResolver } from "@hookform/resolvers/zod";
import { Archive, ArchiveRestore } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Button, Card, CardContent, CardFooter, CardHeader, ConfirmDialog, Typography } from "@/components";
import { useChangeDeviceActiveMutation, useUpdateDeviceMutation } from "../../api";
import { DeviceFormFields } from "../../form/device-form-fields";
import { DEFAULT_DEVICE_FORM_VALUES, deviceFormSchema, type DeviceFormValues } from "../../form/schema";
import { useDeviceOutlet } from "../device-outlet";

export const SectionSettings = () => {
  const { device } = useDeviceOutlet();
  const [archiveOpen, setArchiveOpen] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<DeviceFormValues>({
    resolver: zodResolver(deviceFormSchema),
    defaultValues: DEFAULT_DEVICE_FORM_VALUES,
  });
  const { mutateAsync: updateDevice, isPending: isSaving } = useUpdateDeviceMutation();
  const { mutate: changeActive, isPending: isChanging } = useChangeDeviceActiveMutation();

  useEffect(() => {
    reset({
      name: device.name,
      host: device.host,
      port: device.port,
      credentialId: device.credential ? String(device.credential.id) : "",
      notes: device.notes ?? "",
    });
  }, [device, reset]);

  const onSubmit = async (values: DeviceFormValues) => {
    await updateDevice({
      id: device.id,
      name: values.name,
      host: values.host,
      port: values.port,
      credentialId: values.credentialId ? Number(values.credentialId) : null,
      notes: values.notes || null,
    });
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex flex-col gap-1">
            <Typography variant="title-md">Acesso e identificação</Typography>
            <Typography variant="body-sm" className="text-muted-foreground">
              Ao salvar com uma credencial, o painel entra no dispositivo e relê as informações do sistema.
            </Typography>
          </div>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent>
            <DeviceFormFields register={register} errors={errors} idPrefix="device-settings" />
          </CardContent>
          <CardFooter>
            <Button type="submit" loading={isSaving} disabled={!isDirty}>
              {isSaving ? "Salvando..." : "Salvar"}
            </Button>
          </CardFooter>
        </form>
      </Card>

      <Card className="border-destructive-soft">
        <CardContent className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-col gap-1">
            <Typography variant="title-sm">{device.active ? "Arquivar dispositivo" : "Reativar dispositivo"}</Typography>
            <Typography variant="body-sm" className="text-muted-foreground">
              {device.active
                ? "Some das listas e deixa de ser monitorado. Backups e histórico continuam guardados."
                : "Volta às listas e à detecção automática."}
            </Typography>
          </div>
          {device.active ? (
            <Button variant="destructive" onClick={() => setArchiveOpen(true)}>
              <Archive />
              Arquivar
            </Button>
          ) : (
            <Button variant="outline" onClick={() => changeActive({ id: device.id, active: true })} loading={isChanging}>
              <ArchiveRestore />
              Reativar
            </Button>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={archiveOpen}
        onOpenChange={setArchiveOpen}
        title={`Arquivar ${device.name}?`}
        description="Você pode reativar depois pela lista de arquivados."
        confirmLabel="Arquivar"
        destructive
        loading={isChanging}
        onConfirm={() => changeActive({ id: device.id, active: false }, { onSettled: () => setArchiveOpen(false) })}
      />
    </>
  );
};
