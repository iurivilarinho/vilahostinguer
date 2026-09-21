import { z } from "zod";

const path = z
  .string()
  .trim()
  .regex(/^$|^\/[A-Za-z0-9._/-]+$/, "Use um caminho absoluto sem espaços, como /mnt/dados");

export const attachFormSchema = z
  .object({
    target: z.enum(["DEVICE", "MACHINE"]),
    deviceId: z.string(),
    machineId: z.string(),
    mountPath: path,
    containerPath: path,
  })
  .superRefine((values, context) => {
    if (values.target === "DEVICE" && !values.deviceId) {
      context.addIssue({ code: z.ZodIssueCode.custom, path: ["deviceId"], message: "Escolha o dispositivo" });
    }
    if (values.target === "MACHINE" && !values.machineId) {
      context.addIssue({ code: z.ZodIssueCode.custom, path: ["machineId"], message: "Escolha a máquina" });
    }
    if (values.target === "MACHINE" && !values.containerPath) {
      context.addIssue({ code: z.ZodIssueCode.custom, path: ["containerPath"], message: "Informe onde o disco aparece na máquina" });
    }
  });

export type AttachFormValues = z.infer<typeof attachFormSchema>;

export const DEFAULT_ATTACH_FORM_VALUES: AttachFormValues = {
  target: "DEVICE",
  deviceId: "",
  machineId: "",
  mountPath: "",
  containerPath: "/dados",
};
