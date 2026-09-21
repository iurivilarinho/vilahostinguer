import { z } from "zod";

export const domainFormSchema = z
  .object({
    mode: z.enum(["create", "edit"]),
    name: z
      .string()
      .trim()
      .min(1, "Informe o domínio")
      .max(253, "Domínio longo demais")
      .regex(/^[A-Za-z0-9.-]+$/, "Use só letras, números, ponto e hífen"),
    provider: z.enum(["CLOUDFLARE", "DUCKDNS", "CUSTOM_URL", "MANUAL"]),
    secret: z.string().max(2048, "Até 2048 caracteres"),
    wildcard: z.boolean(),
    ddnsEnabled: z.boolean(),
    intervalMinutes: z.coerce.number({ invalid_type_error: "Informe os minutos" }).int("Número inteiro").min(1, "Mínimo de 1 minuto").max(1440, "Máximo de 1440 minutos"),
    keepSecret: z.boolean(),
  })
  .superRefine((values, context) => {
    const needsSecret = values.provider !== "MANUAL" && !(values.mode === "edit" && values.keepSecret);
    if (needsSecret && values.secret.trim() === "") {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["secret"],
        message: values.provider === "CUSTOM_URL" ? "Informe a URL de atualização" : "Informe o token",
      });
    }
    if (values.provider === "CUSTOM_URL" && values.secret.trim() !== "" && !/^https?:\/\//.test(values.secret.trim())) {
      context.addIssue({ code: z.ZodIssueCode.custom, path: ["secret"], message: "A URL começa com http:// ou https://" });
    }
    if (values.provider === "DUCKDNS" && values.name.includes(".") && !values.name.toLowerCase().endsWith(".duckdns.org")) {
      context.addIssue({ code: z.ZodIssueCode.custom, path: ["name"], message: "Use só o subdomínio (casa) ou o nome completo em duckdns.org" });
    }
  });

export type DomainFormValues = z.infer<typeof domainFormSchema>;

export const DEFAULT_DOMAIN_FORM_VALUES: DomainFormValues = {
  mode: "create",
  name: "",
  provider: "DUCKDNS",
  secret: "",
  wildcard: true,
  ddnsEnabled: true,
  intervalMinutes: 30,
  keepSecret: false,
};
