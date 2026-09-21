import { z } from "zod";

export const credentialFormSchema = z
  .object({
    mode: z.enum(["create", "edit"]),
    name: z.string().trim().min(1, "Informe o nome").max(120, "Use até 120 caracteres"),
    username: z.string().trim().min(1, "Informe o usuário").max(64, "Use até 64 caracteres"),
    authType: z.enum(["PASSWORD", "PRIVATE_KEY"]),
    secret: z.string(),
    passphrase: z.string(),
    defaultCredential: z.boolean(),
  })
  .superRefine((values, context) => {
    if (values.mode === "create" && values.secret.length === 0) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["secret"],
        message: values.authType === "PASSWORD" ? "Informe a senha" : "Cole a chave privada",
      });
    }
    if (values.authType === "PRIVATE_KEY" && values.secret.length > 0 && !values.secret.includes("PRIVATE KEY")) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["secret"],
        message: "Cole a chave inteira, com as linhas BEGIN e END",
      });
    }
  });

export type CredentialFormValues = z.infer<typeof credentialFormSchema>;

export const DEFAULT_CREDENTIAL_FORM_VALUES: CredentialFormValues = {
  mode: "create",
  name: "",
  username: "root",
  authType: "PASSWORD",
  secret: "",
  passphrase: "",
  defaultCredential: false,
};
