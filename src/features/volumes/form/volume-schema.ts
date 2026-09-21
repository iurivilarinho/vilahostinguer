import { z } from "zod";

export const volumeFormSchema = z.object({
  name: z
    .string()
    .trim()
    .regex(/^[a-z0-9][a-z0-9-]{1,29}$/, "Use de 2 a 30 letras minúsculas, números ou hífen, começando por letra ou número"),
  drive: z.string().min(1, "Escolha o disco do PC"),
  sizeGb: z.coerce.number({ invalid_type_error: "Informe o tamanho" }).int("Use um número inteiro").min(1, "O mínimo é 1 GB").max(16384, "O máximo é 16 TB"),
});

export type VolumeFormValues = z.infer<typeof volumeFormSchema>;

export const DEFAULT_VOLUME_FORM_VALUES: VolumeFormValues = { name: "", drive: "", sizeGb: 50 };
