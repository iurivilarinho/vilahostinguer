import { z } from "zod";

export const checkoutSchema = z
  .object({
    cycle: z.enum(["MONTHLY", "QUARTERLY", "SEMIANNUAL", "ANNUAL"]),
    distribution: z.enum(["UBUNTU", "DEBIAN", "ALPINE", "FEDORA", "ROCKY", "ARCH"]),
    version: z.string().min(1, "Escolha a versão"),
    hostname: z
      .string()
      .trim()
      .regex(/^[a-z0-9][a-z0-9-]{1,24}$/, "Use letras minúsculas, números e hífen (2 a 25 caracteres)"),
    username: z
      .string()
      .trim()
      .regex(/^[a-z_][a-z0-9_-]{0,31}$/, "Usuário Linux: minúsculas, números, _ e -"),
    password: z.string().min(8, "Use ao menos 8 caracteres").max(128, "Até 128 caracteres"),
    confirmation: z.string(),
  })
  .refine((values) => values.password === values.confirmation, { path: ["confirmation"], message: "As senhas não conferem" });

export type CheckoutFormValues = z.infer<typeof checkoutSchema>;

export const DEFAULT_CHECKOUT_VALUES: CheckoutFormValues = {
  cycle: "ANNUAL",
  distribution: "UBUNTU",
  version: "24.04",
  hostname: "",
  username: "",
  password: "",
  confirmation: "",
};
