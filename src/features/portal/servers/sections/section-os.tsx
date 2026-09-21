import { zodResolver } from "@hookform/resolvers/zod";
import { AlertTriangle, Disc3 } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Button, Card, CardContent, CardHeader, ConfirmDialog, FieldWrapper, Input, Select, Switch, Typography } from "@/components";
import { OptionCard } from "../../checkout/components/option-card";
import { useReinstallServerMutation, useServerDistributionsQuery, type ServerDto } from "../api";
import { reinstallSchema, type ReinstallFormValues } from "../form/schemas";

type SectionOsProps = {
  server: ServerDto;
  onOperation: (operationId: number) => void;
};

/** Sistema operacional: reinstalar do zero, no mesmo ou em outro sistema. */
export const SectionOs = ({ server, onOperation }: SectionOsProps) => {
  const [pending, setPending] = useState<ReinstallFormValues | null>(null);
  const active = server.subscriptionStatus === "ACTIVE";
  const { data: distributions } = useServerDistributionsQuery(server.id, active);
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ReinstallFormValues>({
    resolver: zodResolver(reinstallSchema),
    defaultValues: { distribution: server.distribution, version: server.version, password: "", confirmation: "", backupFirst: server.backupSlots > 0 },
  });
  const { mutate: reinstall, isPending } = useReinstallServerMutation({
    onSuccess: (operation) => {
      setPending(null);
      onOperation(operation.id);
    },
  });
  const distribution = watch("distribution");
  const selected = distributions?.find((item) => item.key === distribution);

  useEffect(() => {
    if (selected && !selected.versions.includes(watch("version"))) {
      setValue("version", selected.versions[0]);
    }
  }, [selected, setValue, watch]);

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <div className="flex items-center gap-3">
            <span className="flex size-10 items-center justify-center rounded-lg bg-primary-soft text-primary">
              <Disc3 className="size-5" />
            </span>
            <div className="flex flex-col">
              <Typography variant="caption">Sistema atual</Typography>
              <Typography variant="title-sm">
                {server.distributionName} {server.version}
              </Typography>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-5" onSubmit={handleSubmit((values) => setPending(values))}>
            <Typography variant="title-sm">Trocar ou reinstalar o sistema</Typography>
            <div role="radiogroup" className="grid gap-3 sm:grid-cols-3">
              {(distributions ?? []).map((option) => (
                <OptionCard key={option.key} selected={distribution === option.key} onSelect={() => setValue("distribution", option.key)} disabled={!active}>
                  <Typography variant="ui-header">{option.name}</Typography>
                  <Typography variant="caption">{option.versions.join(" · ")}</Typography>
                </OptionCard>
              ))}
            </div>
            <FieldWrapper label="Versão" htmlFor="os-version" error={errors.version?.message} className="max-w-xs">
              <Select id="os-version" {...register("version")} disabled={!active}>
                {(selected?.versions ?? []).map((version) => (
                  <option key={version} value={version}>
                    {version}
                  </option>
                ))}
              </Select>
            </FieldWrapper>
            <div className="grid gap-4 sm:grid-cols-2">
              <FieldWrapper label={`Nova senha de ${server.username}`} htmlFor="os-password" error={errors.password?.message}>
                <Input id="os-password" type="password" autoComplete="new-password" {...register("password")} disabled={!active} />
              </FieldWrapper>
              <FieldWrapper label="Repita a senha" htmlFor="os-confirmation" error={errors.confirmation?.message}>
                <Input id="os-confirmation" type="password" autoComplete="new-password" {...register("confirmation")} disabled={!active} />
              </FieldWrapper>
            </div>
            <label htmlFor="os-backup" className="flex items-center gap-3">
              <Switch id="os-backup" checked={watch("backupFirst")} onCheckedChange={(value) => setValue("backupFirst", value)} disabled={!active || server.backupSlots === 0} />
              <Typography variant="body-sm" as="span">
                Fazer um backup antes (usa um dos {server.backupSlots} lugares de backup do plano)
              </Typography>
            </label>
            <div className="flex items-start gap-3 rounded-lg border border-warning bg-warning-soft p-3 text-warning-foreground">
              <AlertTriangle className="size-5 shrink-0" />
              <Typography variant="caption" as="p" className="text-warning-foreground">
                Reinstalar apaga tudo o que está no servidor: programas, sites e arquivos. O endereço, as portas e o usuário continuam os mesmos.
              </Typography>
            </div>
            <Button type="submit" variant="destructive" className="self-start" disabled={!active}>
              Reinstalar o servidor
            </Button>
          </form>
        </CardContent>
      </Card>
      <ConfirmDialog
        open={pending !== null}
        onOpenChange={(open) => !open && setPending(null)}
        title={`Reinstalar ${server.hostname}?`}
        description={pending ? `O servidor será apagado e instalado de novo com ${selected?.name ?? pending.distribution} ${pending.version}.` : ""}
        confirmLabel="Reinstalar"
        destructive
        loading={isPending}
        typeToConfirm={server.hostname}
        onConfirm={() => pending && reinstall({ id: server.id, ...pending })}
      />
    </div>
  );
};
