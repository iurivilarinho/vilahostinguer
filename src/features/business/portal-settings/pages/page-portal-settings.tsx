import { zodResolver } from "@hookform/resolvers/zod";
import { ExternalLink, Lock, ShieldCheck } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Badge, Button, Card, CardContent, CardFooter, CardHeader, FieldWrapper, Input, PageHeader, QueryErrorState, Skeleton, Switch, Textarea, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { formatDateTime } from "@/lib/format";
import { useIssueCertificateMutation, usePortalSettingsQuery, useSavePortalSettingsMutation, type CertificateStatus, type PortalSettingsDto } from "../api";
import { portalSettingsSchema, type PortalSettingsFormValues } from "../form/schema";

const CERTIFICATE_TONE: Record<CertificateStatus, "neutral" | "info" | "success" | "destructive"> = {
  NONE: "neutral",
  ISSUING: "info",
  ACTIVE: "success",
  FAILED: "destructive",
};

const LOCAL_PREVIEW = "http://127.0.0.1:8748";

const toForm = (settings: PortalSettingsDto): PortalSettingsFormValues => ({
  enabled: settings.enabled,
  registrationOpen: settings.registrationOpen,
  companyName: settings.companyName,
  hostname: settings.hostname ?? "",
  customerSitesDomain: settings.customerSitesDomain ?? "",
  sshHost: settings.sshHost ?? "",
  portRangeStart: settings.portRangeStart,
  portRangeEnd: settings.portRangeEnd,
  invoiceDaysBefore: settings.invoiceDaysBefore,
  suspendAfterDays: settings.suspendAfterDays,
  cancelAfterDays: settings.cancelAfterDays,
  mercadoPagoAccessToken: "",
  removeMercadoPagoToken: false,
  manualPaymentInstructions: settings.manualPaymentInstructions ?? "",
  supportEmail: settings.supportEmail ?? "",
  acmeEmail: settings.acmeEmail ?? "",
});

const Toggle = ({ id, title, description, checked, onChange }: { id: string; title: string; description: string; checked: boolean; onChange: (value: boolean) => void }) => (
  <div className="flex items-start justify-between gap-4 rounded-lg border border-border p-4">
    <div className="flex flex-col gap-1">
      <Typography variant="ui-header" as="label" htmlFor={id}>
        {title}
      </Typography>
      <Typography variant="caption" as="p">
        {description}
      </Typography>
    </div>
    <Switch id={id} checked={checked} onCheckedChange={onChange} />
  </div>
);

export const PagePortalSettings = () => {
  const { data: settings, isLoading, isError, error, refetch, isFetching } = usePortalSettingsQuery();
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isDirty },
  } = useForm<PortalSettingsFormValues>({ resolver: zodResolver(portalSettingsSchema) });
  const { mutate: save, isPending: isSaving } = useSavePortalSettingsMutation({ onSuccess: (saved) => reset(toForm(saved)) });
  const { mutate: issue, isPending: isIssuing } = useIssueCertificateMutation();

  useEffect(() => {
    if (settings && !isDirty) {
      reset(toForm(settings));
    }
  }, [settings, isDirty, reset]);

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as preferências.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  if (isLoading || !settings) {
    return <Skeleton className="h-96 w-full" />;
  }

  const publicUrl = settings.hostname ? `${settings.certificateStatus === "ACTIVE" ? "https" : "http"}://${settings.hostname}` : null;

  return (
    <>
      <PageHeader
        title="Painel do cliente"
        description="Onde os clientes contratam e gerenciam os servidores deles."
        actions={
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => window.open(LOCAL_PREVIEW, "_blank")}>
              <ExternalLink />
              Abrir aqui
            </Button>
            {publicUrl && settings.enabled && (
              <Button variant="outline" onClick={() => window.open(publicUrl, "_blank")}>
                <ExternalLink />
                Abrir na internet
              </Button>
            )}
          </div>
        }
      />
      <form className="flex flex-col gap-4" onSubmit={handleSubmit((values) => save({ ...values, supportEmail: values.supportEmail || null, acmeEmail: values.acmeEmail || null }))}>
        <Card>
          <CardHeader>
            <Typography variant="title-sm">Publicação</Typography>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <Toggle
              id="portal-enabled"
              title="Publicar o painel"
              description="O gateway atende o endereço abaixo e leva os visitantes ao painel. Redirecione as portas 80 e 443 do roteador para este PC."
              checked={watch("enabled") ?? false}
              onChange={(value) => setValue("enabled", value, { shouldDirty: true })}
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <FieldWrapper label="Nome da empresa" htmlFor="portal-company" error={errors.companyName?.message}>
                <Input id="portal-company" {...register("companyName")} />
              </FieldWrapper>
              <FieldWrapper label="Endereço do painel" htmlFor="portal-hostname" error={errors.hostname?.message} description="Um nome do seu domínio, como painel.suaempresa.com.br">
                <Input id="portal-hostname" placeholder="painel.suaempresa.com.br" {...register("hostname")} />
              </FieldWrapper>
              <FieldWrapper
                label="Domínio dos sites dos clientes"
                htmlFor="portal-sites"
                error={errors.customerSitesDomain?.message}
                description="Cada servidor ganha nome.este-domínio (use um DNS curinga *.)."
              >
                <Input id="portal-sites" placeholder="clientes.suaempresa.com.br" {...register("customerSitesDomain")} />
              </FieldWrapper>
              <FieldWrapper label="Endereço do SSH para os clientes" htmlFor="portal-ssh" error={errors.sshHost?.message} description="Vazio: o endereço do painel ou o IP público.">
                <Input id="portal-ssh" placeholder="ssh.suaempresa.com.br" {...register("sshHost")} />
              </FieldWrapper>
            </div>
            <Toggle
              id="portal-registration"
              title="Aceitar cadastros novos"
              description="Desligado, só quem já tem conta entra."
              checked={watch("registrationOpen") ?? true}
              onChange={(value) => setValue("registrationOpen", value, { shouldDirty: true })}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <Typography variant="title-sm">Cobrança</Typography>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <FieldWrapper
              label="Access token do Mercado Pago"
              htmlFor="portal-mp"
              error={errors.mercadoPagoAccessToken?.message}
              description={
                settings.mercadoPagoConfigured
                  ? "Configurado (cifrado com a sua conta do Windows). Deixe em branco para manter."
                  : "Com ele, o cliente paga por Pix e a confirmação é automática. Sem ele, você confirma cada pagamento à mão."
              }
            >
              <Input id="portal-mp" type="password" autoComplete="off" placeholder="APP_USR-..." {...register("mercadoPagoAccessToken")} />
            </FieldWrapper>
            {settings.mercadoPagoConfigured && (
              <label htmlFor="portal-mp-remove" className="flex items-center gap-3">
                <Switch id="portal-mp-remove" checked={watch("removeMercadoPagoToken") ?? false} onCheckedChange={(value) => setValue("removeMercadoPagoToken", value, { shouldDirty: true })} />
                <Typography variant="body-sm" as="span">
                  Apagar o token guardado
                </Typography>
              </label>
            )}
            <FieldWrapper
              label="Instruções de pagamento manual"
              htmlFor="portal-manual"
              error={errors.manualPaymentInstructions?.message}
              description="Mostradas na fatura quando não há Pix automático: chave Pix, banco, contato."
            >
              <Textarea id="portal-manual" rows={3} {...register("manualPaymentInstructions")} />
            </FieldWrapper>
            <div className="grid gap-4 sm:grid-cols-3">
              <FieldWrapper label="Gerar renovação (dias antes)" htmlFor="portal-invoice-days" error={errors.invoiceDaysBefore?.message}>
                <Input id="portal-invoice-days" type="number" {...register("invoiceDaysBefore")} />
              </FieldWrapper>
              <FieldWrapper label="Suspender após (dias de atraso)" htmlFor="portal-suspend" error={errors.suspendAfterDays?.message}>
                <Input id="portal-suspend" type="number" {...register("suspendAfterDays")} />
              </FieldWrapper>
              <FieldWrapper label="Apagar após (dias de atraso)" htmlFor="portal-cancel" error={errors.cancelAfterDays?.message}>
                <Input id="portal-cancel" type="number" {...register("cancelAfterDays")} />
              </FieldWrapper>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <Typography variant="title-sm">Servidores dos clientes</Typography>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <Typography variant="body-sm" className="text-muted-foreground">
              Cada servidor usa três portas desta faixa no dispositivo e no PC: SSH, e as que levam às portas 80 e 443 dele.
            </Typography>
            <div className="grid gap-4 sm:grid-cols-2">
              <FieldWrapper label="Primeira porta" htmlFor="portal-port-start" error={errors.portRangeStart?.message}>
                <Input id="portal-port-start" type="number" {...register("portRangeStart")} />
              </FieldWrapper>
              <FieldWrapper label="Última porta" htmlFor="portal-port-end" error={errors.portRangeEnd?.message}>
                <Input id="portal-port-end" type="number" {...register("portRangeEnd")} />
              </FieldWrapper>
              <FieldWrapper label="E-mail de suporte" htmlFor="portal-support" error={errors.supportEmail?.message}>
                <Input id="portal-support" type="email" {...register("supportEmail")} />
              </FieldWrapper>
            </div>
          </CardContent>
          <CardFooter>
            <Button type="submit" loading={isSaving} disabled={!isDirty}>
              Salvar
            </Button>
          </CardFooter>
        </Card>
      </form>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-3">
            <span className="flex size-10 items-center justify-center rounded-lg bg-primary-soft text-primary">
              <Lock className="size-5" />
            </span>
            <div className="flex flex-col">
              <Typography variant="title-sm">Certificado HTTPS</Typography>
              <Typography variant="caption">Let's Encrypt, grátis, renovado sozinho 30 dias antes de vencer.</Typography>
            </div>
          </div>
          <Badge tone={CERTIFICATE_TONE[settings.certificateStatus]}>{settings.certificateStatusDescription}</Badge>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          {settings.certificateMessage && <Typography variant="body-sm">{settings.certificateMessage}</Typography>}
          {settings.certificateStatus === "ACTIVE" && (
            <Typography variant="body-sm" className="flex items-center gap-2">
              <ShieldCheck className="size-4 text-success-foreground" />
              {settings.certificateHostname}, válido até {formatDateTime(settings.certificateExpiresAt)}
            </Typography>
          )}
          <Typography variant="caption" as="p">
            Antes de pedir: o endereço do painel precisa apontar para o seu IP público (veja Acesso remoto → Domínios) e a porta 80 do roteador precisa
            chegar a este PC. Sem HTTPS, as senhas dos clientes trafegam sem criptografia.
          </Typography>
          <div className="grid gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
            <FieldWrapper label="E-mail para avisos do Let's Encrypt" htmlFor="portal-acme" error={errors.acmeEmail?.message}>
              <Input id="portal-acme" type="email" {...register("acmeEmail")} />
            </FieldWrapper>
            <Button onClick={() => issue()} loading={isIssuing || settings.certificateStatus === "ISSUING"} disabled={!settings.enabled || !settings.hostname}>
              {settings.certificateStatus === "ACTIVE" ? "Renovar agora" : "Emitir certificado"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </>
  );
};
