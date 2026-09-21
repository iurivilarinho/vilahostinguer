import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Button, Card, CardContent, CardFooter, CardHeader, FieldWrapper, Input, Typography } from "@/components";
import { useUpdateGatewaySettingsMutation, type GatewayStatusDto } from "../api";
import { SwitchRow } from "../components/switch-row";
import { DEFAULT_GATEWAY_SETTINGS_VALUES, gatewaySettingsSchema, type GatewaySettingsFormValues } from "./gateway-settings-schema";

type GatewaySettingsCardProps = {
  status?: GatewayStatusDto;
};

export const GatewaySettingsCard = ({ status }: GatewaySettingsCardProps) => {
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isDirty },
  } = useForm<GatewaySettingsFormValues>({
    resolver: zodResolver(gatewaySettingsSchema),
    defaultValues: DEFAULT_GATEWAY_SETTINGS_VALUES,
  });
  const { mutateAsync: save, isPending } = useUpdateGatewaySettingsMutation();

  useEffect(() => {
    if (status && !isDirty) {
      reset({ enabled: status.enabled, httpPort: status.httpPort, tlsPort: status.tlsPort, upnpEnabled: status.upnp.enabled });
    }
  }, [status, isDirty, reset]);

  const onSubmit = async (values: GatewaySettingsFormValues) => {
    const saved = await save(values);
    reset({ enabled: saved.enabled, httpPort: saved.httpPort, tlsPort: saved.tlsPort, upnpEnabled: saved.upnp.enabled });
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-col gap-1">
          <Typography variant="title-sm">Preferências</Typography>
          <Typography variant="caption">Portas que este PC abre para receber as conexões de fora.</Typography>
        </div>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="flex flex-col gap-4">
          <SwitchRow
            id="gateway-enabled"
            title="Receber conexões de fora"
            description="Desligado, nenhuma porta fica aberta e as rotas ficam guardadas."
            checked={watch("enabled")}
            onCheckedChange={(value) => setValue("enabled", value, { shouldDirty: true })}
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <FieldWrapper label="Porta dos sites HTTP" htmlFor="gateway-http-port" error={errors.httpPort?.message} description="Padrão 80. Troque se outro programa já usa.">
              <Input id="gateway-http-port" type="number" inputMode="numeric" {...register("httpPort")} />
            </FieldWrapper>
            <FieldWrapper label="Porta dos sites HTTPS" htmlFor="gateway-tls-port" error={errors.tlsPort?.message} description="Padrão 443.">
              <Input id="gateway-tls-port" type="number" inputMode="numeric" {...register("tlsPort")} />
            </FieldWrapper>
          </div>
          <SwitchRow
            id="gateway-upnp"
            title="Abrir as portas no roteador sozinho (UPnP)"
            description="Só funciona se o UPnP estiver ligado no roteador. As portas abertas são só as das rotas ativas e saem quando o painel fecha."
            checked={watch("upnpEnabled")}
            onCheckedChange={(value) => setValue("upnpEnabled", value, { shouldDirty: true })}
          />
        </CardContent>
        <CardFooter>
          <Button type="submit" loading={isPending} disabled={!isDirty}>
            {isPending ? "Salvando..." : "Salvar preferências"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};
