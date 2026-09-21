import { z } from "zod";

export const backupFormSchema = z.object({
  deviceId: z.string().min(1, "Escolha o dispositivo"),
  name: z.string().trim().min(1, "Informe o nome").max(120, "Use até 120 caracteres"),
  paths: z
    .string()
    .trim()
    .min(1, "Informe ao menos uma pasta")
    .refine(
      (value) =>
        value
          .split("\n")
          .map((line) => line.trim())
          .filter(Boolean)
          .every((line) => line.startsWith("/")),
      "Cada linha deve ser um caminho absoluto, começando por /",
    ),
});

export type BackupFormValues = z.infer<typeof backupFormSchema>;

export const DEFAULT_BACKUP_FORM_VALUES: BackupFormValues = {
  deviceId: "",
  name: "",
  paths: "/etc\n/root\n/home",
};

export const toPathList = (value: string): string[] =>
  value
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
