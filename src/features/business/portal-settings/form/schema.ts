import { z } from "zod";

const hostname = z
  .string()
  .trim()
  .refine((value) => value === "" || /^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z][a-z0-9-]{0,61}[a-z0-9]$/i.test(value), "Endereço inválido");

const integer = (min: number, max: number) =>
  z.coerce.number({ invalid_type_error: "Informe um número" }).int("Número inteiro").min(min, `Mínimo de ${min}`).max(max, `Máximo de ${max}`);

const optionalEmail = z.string().trim().refine((value) => value === "" || z.string().email().safeParse(value).success, "E-mail inválido");

export const portalSettingsSchema = z
  .object({
    enabled: z.boolean(),
    registrationOpen: z.boolean(),
    companyName: z.string().trim().min(1, "Informe o nome da empresa").max(60, "Até 60 caracteres"),
    hostname,
    customerSitesDomain: hostname,
    sshHost: hostname,
    portRangeStart: integer(1024, 65000),
    portRangeEnd: integer(1024, 65535),
    invoiceDaysBefore: integer(1, 30),
    suspendAfterDays: integer(0, 60),
    cancelAfterDays: integer(1, 180),
    mercadoPagoAccessToken: z.string().max(200, "Até 200 caracteres"),
    removeMercadoPagoToken: z.boolean(),
    manualPaymentInstructions: z.string().max(2000, "Até 2000 caracteres"),
    supportEmail: optionalEmail,
    acmeEmail: optionalEmail,
  })
  .refine((values) => !values.enabled || values.hostname !== "", { path: ["hostname"], message: "Informe o endereço para publicar" })
  .refine((values) => values.portRangeEnd - values.portRangeStart >= 2, { path: ["portRangeEnd"], message: "A faixa precisa ter ao menos 3 portas" })
  .refine((values) => values.cancelAfterDays > values.suspendAfterDays, { path: ["cancelAfterDays"], message: "Precisa ser depois da suspensão" });

export type PortalSettingsFormValues = z.infer<typeof portalSettingsSchema>;
