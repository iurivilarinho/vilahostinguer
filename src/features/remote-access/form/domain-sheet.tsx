import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Button, FieldWrapper, Input, Select, Typography } from "@/components";
import { useCreateDomainMutation, useUpdateDomainMutation, type DnsProvider, type DomainDto } from "../api";
import { SwitchRow } from "../components/switch-row";
import { DEFAULT_DOMAIN_FORM_VALUES, domainFormSchema, type DomainFormValues } from "./domain-schema";

type DomainSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  domain?: DomainDto;
};

type ProviderHelp = {
  namePlaceholder: string;
  nameDescription: string;
  secretLabel: string;
  secretPlaceholder: string;
  secretDescription: string;
};

const PROVIDER_HELP: Record<DnsProvider, ProviderHelp> = {
  DUCKDNS: {
    namePlaceholder: "casa",
    nameDescription: "O subdomínio criado em duckdns.org (grátis). Todo nome abaixo dele, como blog.casa.duckdns.org, também aponta para você.",
    secretLabel: "Token do DuckDNS",
    secretPlaceholder: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    secretDescription: "Aparece no topo da página do DuckDNS depois do login.",
  },
  CLOUDFLARE: {
    namePlaceholder: "casa.meudominio.com.br",
    nameDescription: "O domínio (ou subdomínio) cuja zona está na sua conta da Cloudflare.",
    secretLabel: "API Token da Cloudflare",
    secretPlaceholder: "Token com permissão Zone → DNS → Edit",
    secretDescription: "Crie em My Profile → API Tokens → modelo “Edit zone DNS”. Não use a Global API Key.",
  },
  CUSTOM_URL: {
    namePlaceholder: "meuservidor.no-ip.org",
    nameDescription: "O nome mantido pelo serviço de DDNS.",
    secretLabel: "URL de atualização",
    secretPlaceholder: "https://servico/update?host=...&senha=...&ip={ip}",
    secretDescription: "Chamada a cada atualização; {ip} é trocado pelo IP público. Fica cifrada como uma senha.",
  },
  MANUAL: {
    namePlaceholder: "meudominio.com.br",
    nameDescription: "Você mantém o registro A no provedor; o painel confere se ele aponta para o seu IP público.",
    secretLabel: "",
    secretPlaceholder: "",
    secretDescription: "",
  },
};

export const DomainSheet = ({ open, onOpenChange, domain }: DomainSheetProps) => {
  const mode = domain ? "edit" : "create";
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<DomainFormValues>({
    resolver: zodResolver(domainFormSchema),
    defaultValues: DEFAULT_DOMAIN_FORM_VALUES,
  });
  const { mutateAsync: createDomain, isPending: isCreating } = useCreateDomainMutation();
  const { mutateAsync: updateDomain, isPending: isUpdating } = useUpdateDomainMutation();
  const isSaving = isCreating || isUpdating;
  const provider = watch("provider");
  const help = PROVIDER_HELP[provider];
  const keepSecret = mode === "edit" && domain?.hasSecret === true && domain.provider === provider;

  useEffect(() => {
    if (open && domain) {
      reset({
        mode: "edit",
        name: domain.name,
        provider: domain.provider,
        secret: "",
        wildcard: domain.wildcard,
        ddnsEnabled: domain.ddnsEnabled,
        intervalMinutes: domain.intervalMinutes,
        keepSecret: domain.hasSecret,
      });
      return;
    }
    if (open) {
      reset(DEFAULT_DOMAIN_FORM_VALUES);
    }
  }, [open, domain, reset]);

  useEffect(() => {
    setValue("keepSecret", keepSecret);
  }, [keepSecret, setValue]);

  const onSubmit = async (values: DomainFormValues) => {
    const payload = {
      name: values.name,
      provider: values.provider,
      secret: values.provider === "MANUAL" ? "" : values.secret,
      wildcard: values.provider === "CLOUDFLARE" && values.wildcard,
      ddnsEnabled: values.ddnsEnabled,
      intervalMinutes: values.intervalMinutes,
    };
    if (mode === "create") {
      await createDomain(payload);
    }
    if (mode === "edit" && domain) {
      await updateDomain({ id: domain.id, ...payload });
    }
    onOpenChange(false);
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title={mode === "create" ? "Novo domínio" : "Editar domínio"}
      description="Um nome fixo para a sua rede, mesmo que a operadora troque o IP. O token é cifrado com a sua conta do Windows."
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form="domain-form" loading={isSaving}>
            {isSaving ? "Salvando..." : "Salvar"}
          </Button>
        </>
      }
    >
      <form id="domain-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <FieldWrapper label="Onde está o DNS" htmlFor="domain-provider">
          <Select id="domain-provider" {...register("provider")}>
            <option value="DUCKDNS">DuckDNS (grátis)</option>
            <option value="CLOUDFLARE">Cloudflare</option>
            <option value="CUSTOM_URL">Outro serviço de DDNS (URL de atualização)</option>
            <option value="MANUAL">Manual (só conferir)</option>
          </Select>
        </FieldWrapper>
        <FieldWrapper label="Domínio" htmlFor="domain-name" error={errors.name?.message} description={help.nameDescription}>
          <Input id="domain-name" placeholder={help.namePlaceholder} autoComplete="off" {...register("name")} aria-invalid={Boolean(errors.name)} />
        </FieldWrapper>
        {provider !== "MANUAL" && (
          <FieldWrapper
            label={help.secretLabel}
            htmlFor="domain-secret"
            error={errors.secret?.message}
            description={keepSecret ? "Deixe em branco para manter o atual" : help.secretDescription}
          >
            <Input
              id="domain-secret"
              type="password"
              autoComplete="off"
              placeholder={help.secretPlaceholder}
              className="font-mono"
              {...register("secret")}
              aria-invalid={Boolean(errors.secret)}
            />
          </FieldWrapper>
        )}
        {provider === "CLOUDFLARE" && (
          <SwitchRow
            id="domain-wildcard"
            title="Atualizar também o curinga"
            description={`Mantém *.${watch("name") || "dominio"} no mesmo IP: cada site ganha seu subdomínio sem mexer no DNS de novo.`}
            checked={watch("wildcard")}
            onCheckedChange={(value) => setValue("wildcard", value)}
          />
        )}
        <SwitchRow
          id="domain-ddns"
          title={provider === "MANUAL" ? "Conferir sozinho" : "Atualizar sozinho (DDNS)"}
          description="Quando o IP público muda, o painel percebe no minuto seguinte."
          checked={watch("ddnsEnabled")}
          onCheckedChange={(value) => setValue("ddnsEnabled", value)}
        />
        <FieldWrapper
          label="Repetir a cada (minutos)"
          htmlFor="domain-interval"
          error={errors.intervalMinutes?.message}
          description="Mesmo sem mudança de IP, para o caso de alguém mexer no DNS por fora."
          className="max-w-xs"
        >
          <Input id="domain-interval" type="number" inputMode="numeric" {...register("intervalMinutes")} />
        </FieldWrapper>
        <Typography variant="caption" as="p">
          O IP público é descoberto por api.ipify.org, icanhazip.com ou ifconfig.me.
        </Typography>
      </form>
    </AppSheet>
  );
};
