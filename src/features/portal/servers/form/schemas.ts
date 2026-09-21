import { z } from "zod";

export const reinstallSchema = z
  .object({
    distribution: z.enum(["UBUNTU", "DEBIAN", "ROCKY", "ALMA"]),
    version: z.string().min(1, "Escolha a versão"),
    password: z.string().min(8, "Use ao menos 8 caracteres").max(128, "Até 128 caracteres"),
    confirmation: z.string(),
    backupFirst: z.boolean(),
  })
  .refine((values) => values.password === values.confirmation, { path: ["confirmation"], message: "As senhas não conferem" });

export type ReinstallFormValues = z.infer<typeof reinstallSchema>;

export const serverPasswordSchema = z
  .object({
    password: z.string().min(8, "Use ao menos 8 caracteres").max(128, "Até 128 caracteres"),
    confirmation: z.string(),
  })
  .refine((values) => values.password === values.confirmation, { path: ["confirmation"], message: "As senhas não conferem" });

export type ServerPasswordFormValues = z.infer<typeof serverPasswordSchema>;

export const DEFAULT_SERVER_PASSWORD_VALUES: ServerPasswordFormValues = { password: "", confirmation: "" };

export const cancelSchema = z.object({
  atPeriodEnd: z.boolean(),
  reason: z.string().max(500, "Até 500 caracteres"),
});

export type CancelFormValues = z.infer<typeof cancelSchema>;

export const DEFAULT_CANCEL_VALUES: CancelFormValues = { atPeriodEnd: true, reason: "" };
