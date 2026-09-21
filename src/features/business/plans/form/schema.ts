import { z } from "zod";

const number = (message: string) => z.coerce.number({ invalid_type_error: message });

export const planFormSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome").max(60, "Até 60 caracteres"),
  description: z.string().max(300, "Até 300 caracteres"),
  cpuLimit: number("Informe as CPUs").int("Número inteiro").min(1, "Mínimo de 1").max(64, "Máximo de 64"),
  memoryMb: number("Informe a memória").int("Número inteiro").min(512, "Mínimo de 512 MB").max(262_144, "Máximo de 256 GB"),
  diskGb: number("Informe o disco").int("Número inteiro").min(10, "Mínimo de 10 GB"),
  backupSlots: number("Informe os backups").int("Número inteiro").min(0, "Mínimo de 0").max(30, "Máximo de 30"),
  priceMonthly: number("Informe o preço").min(0, "Preço inválido").max(9_999_999, "Preço alto demais"),
  active: z.boolean(),
  featured: z.boolean(),
  orderNumber: number("Informe a ordem").int("Número inteiro").min(0, "Mínimo de 0"),
});

export type PlanFormValues = z.infer<typeof planFormSchema>;

export const DEFAULT_PLAN_FORM_VALUES: PlanFormValues = {
  name: "",
  description: "",
  cpuLimit: 1,
  memoryMb: 1024,
  diskGb: 20,
  backupSlots: 3,
  priceMonthly: 19.9,
  active: true,
  featured: false,
  orderNumber: 1,
};
