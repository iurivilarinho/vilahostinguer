import { zodResolver } from "@hookform/resolvers/zod";
import { AlertTriangle } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Button, FieldWrapper, Input, Select, Switch, Typography } from "@/components";
import { openOperationViewer } from "@/features/operations";
import { useDistributionsQuery, useReinstallMachineMutation, type MachineDto } from "../api";
import { DEFAULT_REINSTALL_FORM_VALUES, reinstallFormSchema, type ReinstallFormValues } from "./reinstall-schema";

type ReinstallSheetProps = {
  machine: MachineDto | null;
  onClose: () => void;
};

/** Formatar a máquina: sistema do zero, na mesma ou em outra distribuição e versão. */
export const ReinstallSheet = ({ machine, onClose }: ReinstallSheetProps) => {
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ReinstallFormValues>({
    resolver: zodResolver(reinstallFormSchema),
    defaultValues: DEFAULT_REINSTALL_FORM_VALUES,
  });
  const { data: distributions } = useDistributionsQuery({ enabled: machine !== null });
  const { mutateAsync: reinstall, isPending } = useReinstallMachineMutation({ onSuccess: (operation) => openOperationViewer(operation.id) });
  const distribution = watch("distribution");
  const selected = distributions?.find((item) => item.key === distribution);

  useEffect(() => {
    if (machine) {
      reset({ ...DEFAULT_REINSTALL_FORM_VALUES, distribution: machine.distribution, version: machine.version });
    }
  }, [machine, reset]);

  useEffect(() => {
    if (selected && !selected.versions.includes(watch("version"))) {
      setValue("version", selected.versions[0]);
    }
  }, [selected, setValue, watch]);

  const onSubmit = async (values: ReinstallFormValues) => {
    if (!machine) {
      return;
    }
    await reinstall({ id: machine.id, distribution: values.distribution, version: values.version, password: values.password, backupFirst: values.backupFirst });
    onClose();
  };

  return (
    <AppSheet
      open={machine !== null}
      onOpenChange={(open) => !open && onClose()}
      title={machine ? `Reinstalar ${machine.name}` : "Reinstalar"}
      description="Apaga o disco da máquina e instala de novo, do zero. Processadores, memória, endereço, o usuário e o acesso do painel continuam os mesmos."
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={isPending}>
            Cancelar
          </Button>
          <Button type="submit" form="reinstall-form" variant="destructive" loading={isPending}>
            Reinstalar
          </Button>
        </>
      }
    >
      <form id="reinstall-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <div className="flex gap-3 rounded-lg border border-warning bg-warning-soft p-3 text-warning-foreground">
          <AlertTriangle className="size-5 shrink-0" />
          <Typography variant="caption" as="p" className="text-warning-foreground">
            Tudo o que foi instalado ou gravado na máquina é apagado. Discos do PC conectados a ela precisam ser conectados de novo.
          </Typography>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <FieldWrapper label="Distribuição" htmlFor="reinstall-distribution">
            <Select id="reinstall-distribution" {...register("distribution")}>
              {(distributions ?? []).map((item) => (
                <option key={item.key} value={item.key}>
                  {item.name}
                </option>
              ))}
            </Select>
          </FieldWrapper>
          <FieldWrapper label="Versão" htmlFor="reinstall-version" error={errors.version?.message}>
            <Select id="reinstall-version" {...register("version")}>
              {(selected?.versions ?? []).map((version) => (
                <option key={version} value={version}>
                  {version}
                </option>
              ))}
            </Select>
          </FieldWrapper>
        </div>
        <FieldWrapper label={`Nova senha de ${machine?.username ?? "usuário"} (e do sudo)`} htmlFor="reinstall-password" error={errors.password?.message}>
          <Input id="reinstall-password" type="password" autoComplete="new-password" {...register("password")} aria-invalid={Boolean(errors.password)} />
        </FieldWrapper>
        <FieldWrapper label="Repita a senha" htmlFor="reinstall-confirmation" error={errors.confirmation?.message}>
          <Input id="reinstall-confirmation" type="password" autoComplete="new-password" {...register("confirmation")} aria-invalid={Boolean(errors.confirmation)} />
        </FieldWrapper>
        <div className="flex items-start justify-between gap-4 rounded-lg border border-border p-4">
          <div className="flex flex-col gap-1">
            <Typography variant="ui-header" as="label" htmlFor="reinstall-backup">
              Fazer backup antes
            </Typography>
            <Typography variant="caption" as="p">
              Guarda a máquina como está agora. Se o backup falhar, nada é apagado.
            </Typography>
          </div>
          <Switch id="reinstall-backup" checked={watch("backupFirst")} onCheckedChange={(value) => setValue("backupFirst", value)} />
        </div>
      </form>
    </AppSheet>
  );
};
