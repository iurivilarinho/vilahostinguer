import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Button, FieldWrapper, Input, Select, Typography } from "@/components";
import { formatBytes } from "@/lib/format";
import { useCreateVolumeMutation, type HostDiskDto } from "../api";
import { DEFAULT_VOLUME_FORM_VALUES, volumeFormSchema, type VolumeFormValues } from "./volume-schema";

const GIGABYTE = 1024 ** 3;

type VolumeSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  disks: HostDiskDto[];
};

/** Novo disco virtual: um arquivo num disco do PC, do tamanho escolhido, com o espaço reservado. */
export const VolumeSheet = ({ open, onOpenChange, disks }: VolumeSheetProps) => {
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setError,
    formState: { errors },
  } = useForm<VolumeFormValues>({
    resolver: zodResolver(volumeFormSchema),
    defaultValues: DEFAULT_VOLUME_FORM_VALUES,
  });
  const { mutateAsync: createVolume, isPending } = useCreateVolumeMutation();
  const usable = disks.filter((disk) => disk.supported);
  const selected = disks.find((disk) => disk.root === watch("drive"));
  const maxGb = selected ? Math.floor(selected.availableBytes / GIGABYTE) : undefined;

  useEffect(() => {
    if (open) {
      const roomiest = [...usable].sort((first, second) => second.availableBytes - first.availableBytes)[0];
      reset({ ...DEFAULT_VOLUME_FORM_VALUES, drive: roomiest?.root ?? "" });
    }
    // só ao abrir: a lista de discos se atualiza sozinha e não deve trocar a escolha
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, reset]);

  const onSubmit = async (values: VolumeFormValues) => {
    if (maxGb !== undefined && values.sizeGb > maxGb) {
      setError("sizeGb", { type: "validate", message: `Cabem até ${maxGb} GB em ${values.drive}` });
      return;
    }
    await createVolume(values);
    onOpenChange(false);
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title="Novo disco"
      description="Um pedaço de um disco deste PC que os dispositivos e as máquinas enxergam como um disco deles."
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isPending}>
            Cancelar
          </Button>
          <Button type="submit" form="volume-form" loading={isPending} disabled={usable.length === 0}>
            Criar disco
          </Button>
        </>
      }
    >
      <form id="volume-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <FieldWrapper label="Nome" htmlFor="volume-name" error={errors.name?.message} description="Vira o nome da pasta e o rótulo do disco.">
          <Input id="volume-name" placeholder="dados-site" autoComplete="off" {...register("name")} aria-invalid={Boolean(errors.name)} />
        </FieldWrapper>
        <FieldWrapper label="Disco do PC" htmlFor="volume-drive" error={errors.drive?.message}>
          <Select id="volume-drive" {...register("drive")} aria-invalid={Boolean(errors.drive)}>
            {disks.map((disk) => (
              <option key={disk.root} value={disk.root} disabled={!disk.supported || disk.availableBytes < GIGABYTE}>
                {disk.root} {disk.label ? `${disk.label} ` : ""}— {disk.supported ? `${formatBytes(disk.availableBytes)} disponíveis` : disk.unsupportedReason}
              </option>
            ))}
          </Select>
        </FieldWrapper>
        <FieldWrapper
          label="Tamanho (GB)"
          htmlFor="volume-size"
          error={errors.sizeGb?.message}
          description={maxGb !== undefined ? `Cabem até ${maxGb} GB neste disco.` : undefined}
        >
          <Input id="volume-size" type="number" min={1} max={maxGb} {...register("sizeGb")} aria-invalid={Boolean(errors.sizeGb)} />
        </FieldWrapper>
        <Typography variant="caption" as="p">
          O arquivo cresce à medida que o dispositivo grava, mas o tamanho todo fica reservado: o painel nunca promete mais do que o disco do
          PC tem livre.
        </Typography>
      </form>
    </AppSheet>
  );
};
