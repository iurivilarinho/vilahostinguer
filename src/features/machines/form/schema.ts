import { z } from "zod";

const MAX_PORT = 65_535;
const HOST_SSH_PORT = 22;

/** Campo numérico opcional mantido como texto no formulário; vazio significa "sem valor". */
const optionalNumberText = (min: number, max: number, integer: boolean, message: string) =>
  z.string().refine((value) => {
    if (value.trim() === "") {
      return true;
    }
    const number = Number(value.replace(",", "."));
    return Number.isFinite(number) && number >= min && number <= max && (!integer || Number.isInteger(number));
  }, message);

export const toOptionalNumber = (value: string): number | null => (value.trim() === "" ? null : Number(value.replace(",", ".")));

const portSchema = z.object({
  hostPort: z.coerce.number({ invalid_type_error: "Porta" }).int().min(1, "Porta inválida").max(MAX_PORT, "Porta inválida"),
  containerPort: z.coerce.number({ invalid_type_error: "Porta" }).int().min(1, "Porta inválida").max(MAX_PORT, "Porta inválida"),
  protocol: z.enum(["tcp", "udp"]),
});

const volumeSchema = z.object({
  hostPath: z.string().trim().regex(/^\/[^'"`$\\]*$/, "Caminho absoluto, sem aspas"),
  containerPath: z.string().trim().regex(/^\/[^'"`$\\]*$/, "Caminho absoluto, sem aspas"),
});

export const machineFormSchema = z
  .object({
    deviceId: z.string().min(1, "Escolha o dispositivo"),
    name: z
      .string()
      .trim()
      .regex(/^[a-z0-9][a-z0-9-]{1,40}$/, "Use letras minúsculas, números e hífen (2 a 41 caracteres)"),
    distribution: z.enum(["UBUNTU", "DEBIAN", "ALPINE", "FEDORA", "ROCKY", "ARCH"]),
    version: z.string().min(1, "Escolha a versão"),
    cpuLimit: optionalNumberText(0.1, 64, false, "Entre 0,1 e 64"),
    memoryLimitMb: optionalNumberText(32, 262_144, true, "Número inteiro entre 32 e 262144"),
    networkMode: z.enum(["BRIDGE", "HOST"]),
    ports: z.array(portSchema),
    volumes: z.array(volumeSchema),
    username: z
      .string()
      .trim()
      .regex(/^[a-z_][a-z0-9_-]{0,31}$/, "Usuário Linux inválido (minúsculas, números, _ e -)"),
    password: z.string().min(4, "Use ao menos 4 caracteres").max(128, "Use até 128 caracteres"),
    installSsh: z.boolean(),
    sshPort: optionalNumberText(1, MAX_PORT, true, "Porta inválida"),
    autoStart: z.boolean(),
  })
  .superRefine((values, context) => {
    if (values.installSsh && values.networkMode === "HOST" && (toOptionalNumber(values.sshPort) ?? HOST_SSH_PORT) === HOST_SSH_PORT) {
      context.addIssue({ code: z.ZodIssueCode.custom, path: ["sshPort"], message: "Na rede do dispositivo a 22 é dele; use outra, como 2201" });
    }
  });

export type MachineFormValues = z.infer<typeof machineFormSchema>;

export const DEFAULT_MACHINE_FORM_VALUES: MachineFormValues = {
  deviceId: "",
  name: "",
  distribution: "UBUNTU",
  version: "24.04",
  cpuLimit: "",
  memoryLimitMb: "",
  networkMode: "HOST",
  ports: [],
  volumes: [],
  username: "",
  password: "",
  installSsh: true,
  sshPort: "2201",
  autoStart: true,
};
