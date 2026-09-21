import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { AppSheet, Button } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { useCreateDeviceMutation } from "../api";
import { DeviceFormFields } from "./device-form-fields";
import { DEFAULT_DEVICE_FORM_VALUES, deviceFormSchema, type DeviceFormValues } from "./schema";

type AddDeviceSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

/** Cadastro pelo endereço, para dispositivos na rede (os do cabo USB aparecem sozinhos). */
export const AddDeviceSheet = ({ open, onOpenChange }: AddDeviceSheetProps) => {
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DeviceFormValues>({
    resolver: zodResolver(deviceFormSchema),
    defaultValues: DEFAULT_DEVICE_FORM_VALUES,
  });
  const { mutateAsync: createDevice, isPending: isSaving } = useCreateDeviceMutation();

  useEffect(() => {
    if (open) {
      reset(DEFAULT_DEVICE_FORM_VALUES);
    }
  }, [open, reset]);

  const onSubmit = async (values: DeviceFormValues) => {
    const device = await createDevice({
      name: values.name,
      host: values.host,
      port: values.port,
      credentialId: values.credentialId ? Number(values.credentialId) : null,
      notes: values.notes || null,
    });
    onOpenChange(false);
    navigate(Rotas.devices.detail(device.id));
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title="Adicionar pelo endereço"
      description="O painel lê a chave do servidor SSH para identificar o dispositivo e, com uma credencial, as informações do sistema."
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form="add-device-form" loading={isSaving}>
            {isSaving ? "Conectando..." : "Adicionar"}
          </Button>
        </>
      }
    >
      <form id="add-device-form" onSubmit={handleSubmit(onSubmit)}>
        <DeviceFormFields register={register} errors={errors} idPrefix="add-device" />
      </form>
    </AppSheet>
  );
};
