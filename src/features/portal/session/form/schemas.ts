import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().trim().min(1, "Informe o e-mail").email("E-mail inválido"),
  password: z.string().min(1, "Informe a senha"),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

export const DEFAULT_LOGIN_VALUES: LoginFormValues = { email: "", password: "" };

export const registerSchema = z
  .object({
    name: z.string().trim().min(3, "Informe o nome completo").max(120, "Até 120 caracteres"),
    email: z.string().trim().min(1, "Informe o e-mail").email("E-mail inválido").max(160, "Até 160 caracteres"),
    phone: z.string().trim().regex(/^[0-9 ()+-]{0,20}$/, "Telefone inválido"),
    document: z.string().trim().regex(/^[0-9./-]{0,18}$/, "CPF ou CNPJ inválido"),
    password: z.string().min(8, "Use ao menos 8 caracteres").max(128, "Até 128 caracteres"),
    confirmation: z.string(),
    acceptTerms: z.boolean().refine((value) => value, "É preciso aceitar os termos de uso"),
  })
  .refine((values) => values.password === values.confirmation, { path: ["confirmation"], message: "As senhas não conferem" });

export type RegisterFormValues = z.infer<typeof registerSchema>;

export const DEFAULT_REGISTER_VALUES: RegisterFormValues = {
  name: "",
  email: "",
  phone: "",
  document: "",
  password: "",
  confirmation: "",
  acceptTerms: false,
};

export const profileSchema = z.object({
  name: z.string().trim().min(3, "Informe o nome completo").max(120, "Até 120 caracteres"),
  email: z.string().trim().min(1, "Informe o e-mail").email("E-mail inválido").max(160, "Até 160 caracteres"),
  phone: z.string().trim().regex(/^[0-9 ()+-]{0,20}$/, "Telefone inválido"),
  document: z.string().trim().regex(/^[0-9./-]{0,18}$/, "CPF ou CNPJ inválido"),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;

export const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Informe a senha atual"),
    newPassword: z.string().min(8, "Use ao menos 8 caracteres").max(128, "Até 128 caracteres"),
    confirmation: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmation, { path: ["confirmation"], message: "As senhas não conferem" });

export type PasswordFormValues = z.infer<typeof passwordSchema>;

export const DEFAULT_PASSWORD_VALUES: PasswordFormValues = { currentPassword: "", newPassword: "", confirmation: "" };
