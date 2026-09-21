import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button, Dialog, FieldWrapper, Input, Select, Typography } from "@/components";
import { openOperationViewer } from "@/features/operations";
import { formatBytes } from "@/lib/format";
import { useFormatPartitionMutation, type PartitionDto } from "../api";

const formatSchema = z.object({
  fileSystem: z.enum(["EXT4", "VFAT"]),
  label: z
    .string()
    .trim()
    .regex(/^[a-zA-Z0-9_-]{0,16}$/, "Use até 16 letras, números, - ou _"),
  confirmation: z.string().trim(),
});

type FormatFormValues = z.infer<typeof formatSchema>;

const DEFAULT_FORMAT_FORM_VALUES: FormatFormValues = { fileSystem: "EXT4", label: "dados", confirmation: "" };

type FormatPartitionDialogProps = {
  deviceId: number;
  partition: PartitionDto | null;
  onClose: () => void;
};

export const FormatPartitionDialog = ({ deviceId, partition, onClose }: FormatPartitionDialogProps) => {
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setError,
    formState: { errors },
  } = useForm<FormatFormValues>({
    resolver: zodResolver(formatSchema),
    defaultValues: DEFAULT_FORMAT_FORM_VALUES,
  });
  const { mutateAsync: formatPartition, isPending: isFormatting } = useFormatPartitionMutation({
    onSuccess: (operation) => openOperationViewer(operation.id),
  });
  const confirmation = watch("confirmation");

  useEffect(() => {
    if (partition) {
      reset(DEFAULT_FORMAT_FORM_VALUES);
    }
  }, [partition, reset]);

  const onSubmit = async (values: FormatFormValues) => {
    if (!partition) {
      return;
    }
    if (values.confirmation !== partition.name) {
      setError("confirmation", { type: "validate", message: "Digite o nome exato da partição" });
      return;
    }
    await formatPartition({ deviceId, partition: partition.name, fileSystem: values.fileSystem, label: values.label, confirmation: values.confirmation });
    onClose();
  };

  return (
    <Dialog
      open={partition !== null}
      onOpenChange={(open) => !open && onClose()}
      title={partition ? `Formatar ${partition.name}` : "Formatar"}
      description={partition ? `${formatBytes(partition.sizeBytes)}${partition.fileSystem ? ` · hoje em ${partition.fileSystem}` : ""}` : undefined}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={isFormatting}>
            Cancelar
          </Button>
          <Button type="submit" form="format-form" variant="destructive" loading={isFormatting} disabled={confirmation !== partition?.name}>
            Formatar
          </Button>
        </>
      }
    >
      <form id="format-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <div className="rounded-lg border border-destructive-soft bg-destructive-soft/50 p-3">
          <Typography variant="body-sm" className="text-destructive-foreground">
            Todos os dados desta partição serão apagados. Não há como desfazer.
          </Typography>
        </div>
        <FieldWrapper label="Sistema de arquivos" htmlFor="format-fs">
          <Select id="format-fs" {...register("fileSystem")}>
            <option value="EXT4">ext4 — para Linux</option>
            <option value="VFAT">FAT32 — lido também por Windows e câmeras</option>
          </Select>
        </FieldWrapper>
        <FieldWrapper label="Rótulo" htmlFor="format-label" error={errors.label?.message}>
          <Input id="format-label" {...register("label")} aria-invalid={Boolean(errors.label)} />
        </FieldWrapper>
        <FieldWrapper label={`Digite ${partition?.name ?? ""} para confirmar`} htmlFor="format-confirmation" error={errors.confirmation?.message}>
          <Input id="format-confirmation" autoComplete="off" className="font-mono" {...register("confirmation")} aria-invalid={Boolean(errors.confirmation)} />
        </FieldWrapper>
      </form>
    </Dialog>
  );
};
