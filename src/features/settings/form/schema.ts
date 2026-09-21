import { z } from "zod";

const MIN_INTERVAL = 2;
const MAX_INTERVAL = 600;

export const settingsFormSchema = z.object({
  scanEnabled: z.boolean(),
  scanIntervalSeconds: z.coerce
    .number({ invalid_type_error: "Informe o intervalo" })
    .int("Use segundos inteiros")
    .min(MIN_INTERVAL, `Mínimo de ${MIN_INTERVAL} segundos`)
    .max(MAX_INTERVAL, `Máximo de ${MAX_INTERVAL} segundos`),
  extraHosts: z.string(),
  autoSetup: z.boolean(),
  backupDirectory: z.string().trim().min(1, "Informe a pasta dos backups"),
});

export type SettingsFormValues = z.infer<typeof settingsFormSchema>;

export const DEFAULT_SETTINGS_FORM_VALUES: SettingsFormValues = {
  scanEnabled: true,
  scanIntervalSeconds: 5,
  extraHosts: "",
  autoSetup: true,
  backupDirectory: "",
};

export const toHostList = (value: string): string[] =>
  value
    .split(/[\n,]/)
    .map((host) => host.trim())
    .filter(Boolean);
