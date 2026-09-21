import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import {
  Button,
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  FieldWrapper,
  Input,
  PageHeader,
  QueryErrorState,
  Skeleton,
  Switch,
  Textarea,
  Typography,
} from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { useScanStatusQuery } from "@/features/devices/api";
import { useSettingsQuery, useUpdateSettingsMutation } from "./api";
import { DEFAULT_SETTINGS_FORM_VALUES, settingsFormSchema, toHostList, type SettingsFormValues } from "./form/schema";

export const PageSettings = () => {
  const { data: settings, isLoading, isError, error, refetch, isFetching } = useSettingsQuery();
  const { data: scan } = useScanStatusQuery();
  const { mutateAsync: updateSettings, isPending: isSaving } = useUpdateSettingsMutation();
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isDirty },
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsFormSchema),
    defaultValues: DEFAULT_SETTINGS_FORM_VALUES,
  });

  useEffect(() => {
    if (settings) {
      reset({
        scanEnabled: settings.scanEnabled,
        scanIntervalSeconds: settings.scanIntervalSeconds,
        extraHosts: settings.extraHosts.join("\n"),
        autoSetup: settings.autoSetup,
        backupDirectory: settings.backupDirectory,
      });
    }
  }, [settings, reset]);

  const onSubmit = async (values: SettingsFormValues) => {
    await updateSettings({
      scanEnabled: values.scanEnabled,
      scanIntervalSeconds: values.scanIntervalSeconds,
      extraHosts: toHostList(values.extraHosts),
      autoSetup: values.autoSetup,
      backupDirectory: values.backupDirectory,
    });
  };

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as configurações.")} onRetry={() => refetch()} retrying={isFetching} />;
  }

  return (
    <>
      <PageHeader title="Configurações" description="Detecção automática e onde os backups ficam guardados." />
      {isLoading ? (
        <Skeleton className="h-96 w-full" />
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6">
          <Card>
            <CardHeader>
              <div className="flex flex-col gap-1">
                <Typography variant="title-md">Detecção de dispositivos</Typography>
                <Typography variant="body-sm" className="text-muted-foreground">
                  O painel procura aparelhos nos adaptadores de rede USB (endereços 169.254.x.x) e nos endereços extras abaixo.
                </Typography>
              </div>
            </CardHeader>
            <CardContent className="flex flex-col gap-5">
              <div className="flex items-start justify-between gap-4">
                <div className="flex flex-col gap-1">
                  <Typography variant="ui-header" as="label" htmlFor="settings-scan-enabled">
                    Procurar automaticamente
                  </Typography>
                  <Typography variant="caption" as="p">
                    Mantém a lista em dia e avisa quando um dispositivo conecta ou desconecta.
                  </Typography>
                </div>
                <Switch id="settings-scan-enabled" checked={watch("scanEnabled")} onCheckedChange={(value) => setValue("scanEnabled", value, { shouldDirty: true })} />
              </div>
              <div className="flex items-start justify-between gap-4">
                <div className="flex flex-col gap-1">
                  <Typography variant="ui-header" as="label" htmlFor="settings-auto-setup">
                    Configurar dispositivos novos sozinho
                  </Typography>
                  <Typography variant="caption" as="p">
                    Vincula a credencial padrão e lê as informações do sistema assim que o aparelho aparece.
                  </Typography>
                </div>
                <Switch id="settings-auto-setup" checked={watch("autoSetup")} onCheckedChange={(value) => setValue("autoSetup", value, { shouldDirty: true })} />
              </div>
              <FieldWrapper label="Intervalo entre buscas (segundos)" htmlFor="settings-interval" error={errors.scanIntervalSeconds?.message} className="max-w-xs">
                <Input id="settings-interval" type="number" inputMode="numeric" {...register("scanIntervalSeconds")} aria-invalid={Boolean(errors.scanIntervalSeconds)} />
              </FieldWrapper>
              <FieldWrapper
                label="Endereços extras"
                htmlFor="settings-hosts"
                description="Um por linha, com porta opcional: 192.168.0.50 ou 192.168.0.60:2222"
              >
                <Textarea id="settings-hosts" rows={4} className="font-mono text-xs" {...register("extraHosts")} />
              </FieldWrapper>
              {scan && (
                <div className="rounded-lg bg-muted/60 p-4">
                  <Typography variant="ui-header">Última busca</Typography>
                  <Typography variant="caption" as="p">
                    Adaptadores USB: {scan.usbInterfaces.length ? scan.usbInterfaces.join(", ") : "nenhum"}
                  </Typography>
                  <Typography variant="caption" as="p">
                    Verificados: {scan.probedHosts.length ? scan.probedHosts.join(", ") : "nenhum"} · Com SSH:{" "}
                    {scan.reachableHosts.length ? scan.reachableHosts.join(", ") : "nenhum"}
                  </Typography>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div className="flex flex-col gap-1">
                <Typography variant="title-md">Backups</Typography>
                <Typography variant="body-sm" className="text-muted-foreground">
                  Cada dispositivo ganha uma subpasta com o número dele.
                </Typography>
              </div>
            </CardHeader>
            <CardContent>
              <FieldWrapper label="Pasta dos backups" htmlFor="settings-backup-dir" error={errors.backupDirectory?.message}>
                <Input id="settings-backup-dir" className="font-mono" {...register("backupDirectory")} aria-invalid={Boolean(errors.backupDirectory)} />
              </FieldWrapper>
            </CardContent>
            <CardFooter>
              <Button variant="outline" onClick={() => settings && reset()} disabled={!isDirty || isSaving}>
                Descartar alterações
              </Button>
              <Button type="submit" loading={isSaving} disabled={!isDirty}>
                {isSaving ? "Salvando..." : "Salvar"}
              </Button>
            </CardFooter>
          </Card>
        </form>
      )}
    </>
  );
};
