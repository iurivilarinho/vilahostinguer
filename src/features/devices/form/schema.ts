import { z } from "zod";

const MAX_PORT = 65_535;

export const deviceFormSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome").max(120, "Use até 120 caracteres"),
  host: z.string().trim().min(1, "Informe o endereço").max(255, "Endereço longo demais"),
  port: z.coerce.number({ invalid_type_error: "Informe a porta" }).int("Porta inválida").min(1, "Porta inválida").max(MAX_PORT, "Porta inválida"),
  credentialId: z.string(),
  notes: z.string().max(4000, "Use até 4000 caracteres"),
});

export type DeviceFormValues = z.infer<typeof deviceFormSchema>;

export const DEFAULT_DEVICE_FORM_VALUES: DeviceFormValues = {
  name: "",
  host: "",
  port: 22,
  credentialId: "",
  notes: "",
};
