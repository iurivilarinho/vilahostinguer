import { z } from "zod";

const whole = (message: string) => z.coerce.number({ invalid_type_error: message }).int(message);

export const machineFormSchema = z.object({
  name: z
    .string()
    .trim()
    .regex(/^[a-z0-9][a-z0-9-]{1,40}$/, "Use letras minúsculas, números e hífen (2 a 41 caracteres)"),
  distribution: z.enum(["UBUNTU", "DEBIAN", "ROCKY", "ALMA"]),
  version: z.string().min(1, "Escolha a versão"),
  cpuCount: whole("Número inteiro").min(1, "Mínimo de 1").max(64, "Máximo de 64"),
  memoryMb: whole("Número inteiro").min(512, "Mínimo de 512 MB").max(262_144, "Máximo de 256 GB"),
  diskGb: whole("Número inteiro").min(10, "Mínimo de 10 GB").max(4096, "Máximo de 4 TB"),
  drive: z.string(),
  username: z
    .string()
    .trim()
    .regex(/^[a-z_][a-z0-9_-]{0,31}$/, "Usuário Linux inválido (minúsculas, números, _ e -)"),
  password: z.string().min(4, "Use ao menos 4 caracteres").max(128, "Use até 128 caracteres"),
  autoStart: z.boolean(),
});

export type MachineFormValues = z.infer<typeof machineFormSchema>;

export const DEFAULT_MACHINE_FORM_VALUES: MachineFormValues = {
  name: "",
  distribution: "UBUNTU",
  version: "24.04",
  cpuCount: 2,
  memoryMb: 2048,
  diskGb: 20,
  drive: "",
  username: "",
  password: "",
  autoStart: true,
};
