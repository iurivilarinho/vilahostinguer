import { z } from "zod";

const MAX_PORT = 65_535;

const portText = (message: string) =>
  z.string().refine((value) => {
    const number = Number(value);
    return value.trim() !== "" && Number.isInteger(number) && number >= 1 && number <= MAX_PORT;
  }, message);

export const routeFormSchema = z
  .object({
    type: z.enum(["HTTP", "TLS", "TCP"]),
    hostname: z.string().trim().max(253, "Nome longo demais"),
    publicPort: z.string(),
    /** "machine:3" ou "device:1": um só campo escolhe o destino. */
    target: z.string().min(1, "Escolha o destino"),
    targetPort: portText("Porta inválida"),
    description: z.string().max(200, "Até 200 caracteres"),
  })
  .superRefine((values, context) => {
    if (values.type !== "TCP" && !/^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z][a-z0-9-]{0,61}[a-z0-9]$/i.test(values.hostname)) {
      context.addIssue({ code: z.ZodIssueCode.custom, path: ["hostname"], message: "Informe o nome do site, como blog.casa.duckdns.org" });
    }
    if (values.type === "TCP" && !portText("").safeParse(values.publicPort).success) {
      context.addIssue({ code: z.ZodIssueCode.custom, path: ["publicPort"], message: "Porta inválida" });
    }
  });

export type RouteFormValues = z.infer<typeof routeFormSchema>;

export const DEFAULT_ROUTE_FORM_VALUES: RouteFormValues = {
  type: "HTTP",
  hostname: "",
  publicPort: "",
  target: "",
  targetPort: "80",
  description: "",
};

export const parseTarget = (target: string): { deviceId: number | null; machineId: number | null } => {
  const [kind, id] = target.split(":");
  return kind === "machine" ? { deviceId: null, machineId: Number(id) } : { deviceId: Number(id), machineId: null };
};
