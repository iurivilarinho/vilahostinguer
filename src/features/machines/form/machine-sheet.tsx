import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Button, FieldWrapper, Input, Select, Switch, Typography } from "@/components";
import { openOperationViewer } from "@/features/operations";
import { formatBytes } from "@/lib/format";
import { useCreateMachineMutation, useDistributionsQuery, useMachineHostQuery } from "../api";
import { DEFAULT_MACHINE_FORM_VALUES, machineFormSchema, type MachineFormValues } from "./schema";

const GIGABYTE = 1024 ** 3;

type MachineSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

/** Nova máquina virtual neste PC, a partir da imagem oficial da distribuição. */
export const MachineSheet = ({ open, onOpenChange }: MachineSheetProps) => {
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    setError,
    formState: { errors },
  } = useForm<MachineFormValues>({
    resolver: zodResolver(machineFormSchema),
    defaultValues: DEFAULT_MACHINE_FORM_VALUES,
  });
  const { data: host } = useMachineHostQuery({ enabled: open });
  const { data: distributions } = useDistributionsQuery({ enabled: open });
  const { mutateAsync: createMachine, isPending: isSaving } = useCreateMachineMutation({
    onSuccess: (created) => openOperationViewer(created.operationId),
  });
  const distribution = watch("distribution");
  const selectedDistribution = distributions?.find((item) => item.key === distribution);
  const disks = (host?.disks ?? []).filter((disk) => disk.supported);

  useEffect(() => {
    if (open) {
      reset(DEFAULT_MACHINE_FORM_VALUES);
    }
  }, [open, reset]);

  useEffect(() => {
    if (selectedDistribution && !selectedDistribution.versions.includes(watch("version"))) {
      setValue("version", selectedDistribution.versions[0]);
    }
  }, [selectedDistribution, setValue, watch]);

  const onSubmit = async (values: MachineFormValues) => {
    if (host && values.memoryMb > host.availableMemoryMb) {
      setError("memoryMb", { type: "validate", message: `Sobram ${host.availableMemoryMb} MB para máquinas neste PC` });
      return;
    }
    if (host && values.cpuCount > host.cpus) {
      setError("cpuCount", { type: "validate", message: `Este PC tem ${host.cpus} processadores lógicos` });
      return;
    }
    await createMachine({ ...values, drive: values.drive || undefined });
    onOpenChange(false);
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title="Nova máquina"
      description="Uma máquina virtual Linux neste PC (Hyper-V). Ela ganha um endereço fixo e aparece no painel como um servidor a mais."
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form="machine-form" loading={isSaving} disabled={host !== undefined && !host.ready}>
            Criar máquina
          </Button>
        </>
      }
    >
      <form id="machine-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <FieldWrapper label="Nome" htmlFor="machine-name" error={errors.name?.message} description="Vira o nome da máquina na rede (hostname).">
          <Input id="machine-name" placeholder="web-1" autoComplete="off" {...register("name")} aria-invalid={Boolean(errors.name)} />
        </FieldWrapper>

        <div className="grid gap-4 sm:grid-cols-2">
          <FieldWrapper label="Distribuição" htmlFor="machine-distribution">
            <Select id="machine-distribution" {...register("distribution")}>
              {(distributions ?? []).map((item) => (
                <option key={item.key} value={item.key}>
                  {item.name}
                </option>
              ))}
            </Select>
          </FieldWrapper>
          <FieldWrapper label="Versão" htmlFor="machine-version" error={errors.version?.message}>
            <Select id="machine-version" {...register("version")}>
              {(selectedDistribution?.versions ?? []).map((version) => (
                <option key={version} value={version}>
                  {version}
                </option>
              ))}
            </Select>
          </FieldWrapper>
        </div>

        <div className="grid gap-4 sm:grid-cols-3">
          <FieldWrapper label="Processadores" htmlFor="machine-cpus" error={errors.cpuCount?.message} description={host ? `O PC tem ${host.cpus}` : undefined}>
            <Input id="machine-cpus" type="number" min={1} max={host?.cpus} {...register("cpuCount")} aria-invalid={Boolean(errors.cpuCount)} />
          </FieldWrapper>
          <FieldWrapper
            label="Memória (MB)"
            htmlFor="machine-memory"
            error={errors.memoryMb?.message}
            description={host ? `Sobram ${host.availableMemoryMb} MB` : undefined}
          >
            <Input id="machine-memory" type="number" min={512} step={256} {...register("memoryMb")} aria-invalid={Boolean(errors.memoryMb)} />
          </FieldWrapper>
          <FieldWrapper label="Disco (GB)" htmlFor="machine-disk" error={errors.diskGb?.message}>
            <Input id="machine-disk" type="number" min={10} {...register("diskGb")} aria-invalid={Boolean(errors.diskGb)} />
          </FieldWrapper>
        </div>

        <FieldWrapper label="Disco do PC onde a máquina fica" htmlFor="machine-drive" description="O disco da máquina cresce conforme ela grava; o tamanho todo fica reservado.">
          <Select id="machine-drive" {...register("drive")}>
            <option value="">O que tiver mais espaço</option>
            {disks.map((disk) => (
              <option key={disk.root} value={disk.root} disabled={disk.availableBytes < GIGABYTE * 10}>
                {disk.root} {disk.label ? `${disk.label} ` : ""}— {formatBytes(disk.availableBytes)} disponíveis
              </option>
            ))}
          </Select>
        </FieldWrapper>

        <div className="grid gap-4 sm:grid-cols-2">
          <FieldWrapper label="Usuário (com sudo)" htmlFor="machine-user" error={errors.username?.message}>
            <Input id="machine-user" placeholder="admin" autoComplete="off" {...register("username")} aria-invalid={Boolean(errors.username)} />
          </FieldWrapper>
          <FieldWrapper label="Senha (SSH e sudo)" htmlFor="machine-password" error={errors.password?.message}>
            <Input id="machine-password" type="password" autoComplete="new-password" {...register("password")} aria-invalid={Boolean(errors.password)} />
          </FieldWrapper>
        </div>

        <div className="flex items-start justify-between gap-4 rounded-lg border border-border p-4">
          <div className="flex flex-col gap-1">
            <Typography variant="ui-header" as="label" htmlFor="machine-autostart">
              Ligar junto com o PC
            </Typography>
            <Typography variant="caption" as="p">
              Quando o Windows inicia, o Hyper-V liga a máquina sozinho.
            </Typography>
          </div>
          <Switch id="machine-autostart" checked={watch("autoStart")} onCheckedChange={(value) => setValue("autoStart", value)} />
        </div>

        <Typography variant="caption" as="p">
          A primeira máquina de cada versão baixa a imagem oficial da distribuição (de 300 a 700 MB) e confere o checksum; as próximas só
          copiam. O painel entra na máquina como root com uma chave própria, para terminal, aplicativos e backups.
        </Typography>
      </form>
    </AppSheet>
  );
};
