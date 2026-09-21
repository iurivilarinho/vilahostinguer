import { z } from "zod";

export const reinstallFormSchema = z
  .object({
    distribution: z.enum(["UBUNTU", "DEBIAN", "ALPINE", "FEDORA", "ROCKY", "ARCH"]),
    version: z.string().min(1, "Escolha a versão"),
    password: z.string().min(4, "Use ao menos 4 caracteres").max(128, "Use até 128 caracteres"),
    confirmation: z.string(),
    backupFirst: z.boolean(),
  })
  .refine((values) => values.password === values.confirmation, { path: ["confirmation"], message: "As senhas não conferem" });

export type ReinstallFormValues = z.infer<typeof reinstallFormSchema>;

export const DEFAULT_REINSTALL_FORM_VALUES: ReinstallFormValues = {
  distribution: "UBUNTU",
  version: "24.04",
  password: "",
  confirmation: "",
  backupFirst: true,
};
