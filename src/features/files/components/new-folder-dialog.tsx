import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button, Dialog, FieldWrapper, Input } from "@/components";
import { useCreateFolderMutation } from "../api";

const newFolderSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Informe o nome")
    .refine((value) => !value.includes("/"), "Use só o nome, sem barras"),
});

type NewFolderFormValues = z.infer<typeof newFolderSchema>;

const DEFAULT_NEW_FOLDER_FORM_VALUES: NewFolderFormValues = { name: "" };

type NewFolderDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  deviceId: number;
  parentPath: string;
};

export const NewFolderDialog = ({ open, onOpenChange, deviceId, parentPath }: NewFolderDialogProps) => {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<NewFolderFormValues>({
    resolver: zodResolver(newFolderSchema),
    defaultValues: DEFAULT_NEW_FOLDER_FORM_VALUES,
  });
  const { mutateAsync: createFolder, isPending: isSaving } = useCreateFolderMutation();

  useEffect(() => {
    if (open) {
      reset(DEFAULT_NEW_FOLDER_FORM_VALUES);
    }
  }, [open, reset]);

  const onSubmit = async (values: NewFolderFormValues) => {
    const path = parentPath === "/" ? `/${values.name}` : `${parentPath}/${values.name}`;
    await createFolder({ deviceId, path });
    onOpenChange(false);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Nova pasta"
      description={`Dentro de ${parentPath}`}
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form="new-folder-form" loading={isSaving}>
            Criar
          </Button>
        </>
      }
    >
      <form id="new-folder-form" onSubmit={handleSubmit(onSubmit)}>
        <FieldWrapper label="Nome da pasta" htmlFor="new-folder-name" error={errors.name?.message}>
          <Input id="new-folder-name" autoFocus {...register("name")} aria-invalid={Boolean(errors.name)} />
        </FieldWrapper>
      </form>
    </Dialog>
  );
};
