import { zodResolver } from "@hookform/resolvers/zod";
import { ShieldAlert } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Badge, Button, FieldWrapper, Input, Select, Textarea, Typography } from "@/components";
import { useDevicesQuery } from "@/features/devices/api";
import { useMachinesQuery } from "@/features/machines/api";
import { useCreateRouteMutation, useDomainsQuery, useUpdateRouteMutation, type RouteDto } from "../api";
import { DEFAULT_ROUTE_FORM_VALUES, parseTarget, routeFormSchema, type RouteFormValues } from "./route-schema";

const DEVICES_PARAMS = { page: 0, size: 100, filter: { active: true } };
const MACHINES_PARAMS = { page: 0, size: 200, sort: [{ by: "name" as const, direction: "asc" as const }] };
const DOMAINS_PARAMS = { page: 0, size: 50, filter: { active: true } };
const SSH_LIKE_PORTS = [22, 2201, 2222];

export type RouteSheetPreset = Partial<RouteFormValues>;

type RouteSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  route?: RouteDto;
  /** Valores iniciais de uma rota nova, como "publicar o SSH desta máquina". */
  preset?: RouteSheetPreset;
};

const TYPE_HELP: Record<RouteFormValues["type"], string> = {
  HTTP: "Vários sites dividem a mesma porta do PC; o nome digitado no navegador escolhe o destino.",
  TLS: "Para sites com certificado próprio (Let's Encrypt, por exemplo). O PC só repassa: a criptografia fica no destino.",
  TCP: "Uma porta do PC inteira para um serviço: SSH, banco de dados, servidor de jogo...",
};

export const RouteSheet = ({ open, onOpenChange, route, preset }: RouteSheetProps) => {
  const mode = route ? "edit" : "create";
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<RouteFormValues>({
    resolver: zodResolver(routeFormSchema),
    defaultValues: DEFAULT_ROUTE_FORM_VALUES,
  });
  const { data: devices } = useDevicesQuery(DEVICES_PARAMS, { enabled: open });
  const { data: machines } = useMachinesQuery(MACHINES_PARAMS, { enabled: open });
  const { data: domains } = useDomainsQuery(DOMAINS_PARAMS, { enabled: open });
  const { mutateAsync: createRoute, isPending: isCreating } = useCreateRouteMutation();
  const { mutateAsync: updateRoute, isPending: isUpdating } = useUpdateRouteMutation();
  const isSaving = isCreating || isUpdating;
  const type = watch("type");
  const target = watch("target");
  const targetPort = Number(watch("targetPort"));
  const selectedMachine = target.startsWith("machine:") ? machines?.data.find((machine) => `machine:${machine.id}` === target) : undefined;

  useEffect(() => {
    if (!open) {
      return;
    }
    if (route) {
      reset({
        type: route.type,
        hostname: route.hostname ?? "",
        publicPort: route.publicPort ? String(route.publicPort) : "",
        target: route.machine ? `machine:${route.machine.id}` : `device:${route.device.id}`,
        targetPort: String(route.targetPort),
        description: route.description ?? "",
      });
      return;
    }
    reset({ ...DEFAULT_ROUTE_FORM_VALUES, ...preset });
  }, [open, route, preset, reset]);

  const onSubmit = async (values: RouteFormValues) => {
    const payload = {
      type: values.type,
      hostname: values.type === "TCP" ? null : values.hostname,
      publicPort: values.type === "TCP" ? Number(values.publicPort) : null,
      ...parseTarget(values.target),
      targetPort: Number(values.targetPort),
      description: values.description.trim() === "" ? null : values.description.trim(),
    };
    if (mode === "create") {
      await createRoute(payload);
    }
    if (mode === "edit" && route) {
      await updateRoute({ id: route.id, ...payload });
    }
    onOpenChange(false);
  };

  const suggestHostname = (domainName: string) => {
    const prefix = selectedMachine?.name ?? "site";
    setValue("hostname", `${prefix}.${domainName}`, { shouldValidate: true });
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title={mode === "create" ? "Nova rota" : "Editar rota"}
      description="Leva quem chega de fora até um serviço num dispositivo ou numa máquina, passando por este PC."
      className="max-w-xl"
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form="route-form" loading={isSaving}>
            {isSaving ? "Salvando..." : "Salvar"}
          </Button>
        </>
      }
    >
      <form id="route-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6">
        <section className="flex flex-col gap-4">
          <Typography variant="section-label">Entrada</Typography>
          <FieldWrapper label="Tipo" htmlFor="route-type" description={TYPE_HELP[type]}>
            <Select id="route-type" {...register("type")}>
              <option value="HTTP">Site (HTTP)</option>
              <option value="TLS">Site seguro (HTTPS, repassado)</option>
              <option value="TCP">Porta TCP</option>
            </Select>
          </FieldWrapper>
          {type === "TCP" ? (
            <FieldWrapper label="Porta aberta no PC" htmlFor="route-public-port" error={errors.publicPort?.message} className="max-w-xs">
              <Input id="route-public-port" type="number" inputMode="numeric" placeholder="2201" {...register("publicPort")} />
            </FieldWrapper>
          ) : (
            <FieldWrapper label="Nome do site" htmlFor="route-hostname" error={errors.hostname?.message}>
              <Input id="route-hostname" placeholder="blog.casa.duckdns.org" autoComplete="off" {...register("hostname")} aria-invalid={Boolean(errors.hostname)} />
            </FieldWrapper>
          )}
          {type !== "TCP" && (domains?.data.length ?? 0) > 0 && (
            <div className="flex flex-wrap items-center gap-2">
              <Typography variant="caption">Usar um domínio seu:</Typography>
              {domains?.data.map((domain) => (
                <button key={domain.id} type="button" onClick={() => suggestHostname(domain.name)} className="cursor-pointer">
                  <Badge tone="primary">{domain.name}</Badge>
                </button>
              ))}
            </div>
          )}
        </section>

        <section className="flex flex-col gap-4">
          <Typography variant="section-label">Destino</Typography>
          <FieldWrapper label="Máquina ou dispositivo" htmlFor="route-target" error={errors.target?.message}>
            <Select id="route-target" {...register("target")} aria-invalid={Boolean(errors.target)}>
              <option value="">Escolha</option>
              <optgroup label="Máquinas">
                {machines?.data.map((machine) => (
                  <option key={machine.id} value={`machine:${machine.id}`}>
                    {machine.name} — {machine.device.name}
                  </option>
                ))}
              </optgroup>
              <optgroup label="Dispositivos">
                {devices?.data.map((device) => (
                  <option key={device.id} value={`device:${device.id}`}>
                    {device.name} ({device.host})
                  </option>
                ))}
              </optgroup>
            </Select>
          </FieldWrapper>
          <FieldWrapper
            label={selectedMachine ? "Porta do serviço dentro da máquina" : "Porta do serviço no dispositivo"}
            htmlFor="route-target-port"
            error={errors.targetPort?.message}
            className="max-w-xs"
          >
            <Input id="route-target-port" type="number" inputMode="numeric" {...register("targetPort")} />
          </FieldWrapper>
          {selectedMachine?.networkMode === "BRIDGE" && (
            <Typography variant="caption" as="p">
              Esta máquina está em rede isolada: só as portas encaminhadas chegam até ela (
              {selectedMachine.ports.map((port) => port.containerPort).join(", ") || "nenhuma"}).
            </Typography>
          )}
          {selectedMachine?.sshEnabled && selectedMachine.sshPort !== null && (
            <Button
              variant="outline"
              size="sm"
              className="self-start"
              onClick={() => {
                const sshPort = String(selectedMachine.sshPort);
                setValue("type", "TCP");
                setValue("targetPort", sshPort);
                setValue("publicPort", sshPort);
              }}
            >
              Publicar o SSH (porta {selectedMachine.sshPort})
            </Button>
          )}
          {type === "TCP" && SSH_LIKE_PORTS.includes(targetPort) && (
            <div className="flex gap-3 rounded-lg border border-warning bg-warning-soft p-3 text-warning-foreground">
              <ShieldAlert className="size-5 shrink-0" />
              <Typography variant="caption" as="p" className="text-warning-foreground">
                SSH aberto na internet recebe tentativas de senha o tempo todo. Use uma senha longa (ou só chave) nesta máquina.
              </Typography>
            </div>
          )}
          <FieldWrapper label="Observação" htmlFor="route-description" error={errors.description?.message}>
            <Textarea id="route-description" rows={2} {...register("description")} />
          </FieldWrapper>
        </section>
      </form>
    </AppSheet>
  );
};
