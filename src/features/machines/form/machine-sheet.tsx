import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Trash2 } from "lucide-react";
import { useEffect } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { AppSheet, Button, FieldWrapper, Input, Select, Switch, Typography } from "@/components";
import { useDevicesQuery, type DeviceDto } from "@/features/devices/api";
import { openOperationViewer } from "@/features/operations";
import { useCreateMachineMutation, useDistributionsQuery } from "../api";
import { DEFAULT_MACHINE_FORM_VALUES, machineFormSchema, toOptionalNumber, type MachineFormValues } from "./schema";

const READY_DEVICES_PARAMS = { page: 0, size: 100, filter: { active: true, status: ["READY" as const] } };

type MachineSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  device?: DeviceDto;
};

type SwitchRowProps = {
  id: string;
  title: string;
  description: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
};

const SwitchRow = ({ id, title, description, checked, onCheckedChange }: SwitchRowProps) => (
  <div className="flex items-start justify-between gap-4 rounded-lg border border-border p-4">
    <div className="flex flex-col gap-1">
      <Typography variant="ui-header" as="label" htmlFor={id}>
        {title}
      </Typography>
      <Typography variant="caption" as="p">
        {description}
      </Typography>
    </div>
    <Switch id={id} checked={checked} onCheckedChange={onCheckedChange} />
  </div>
);

export const MachineSheet = ({ open, onOpenChange, device }: MachineSheetProps) => {
  const {
    register,
    control,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<MachineFormValues>({
    resolver: zodResolver(machineFormSchema),
    defaultValues: DEFAULT_MACHINE_FORM_VALUES,
  });
  const ports = useFieldArray({ control, name: "ports" });
  const volumes = useFieldArray({ control, name: "volumes" });
  const deviceId = watch("deviceId");
  const distribution = watch("distribution");
  const networkMode = watch("networkMode");
  const installSsh = watch("installSsh");
  const { data: devices } = useDevicesQuery(READY_DEVICES_PARAMS, { enabled: open && !device });
  const { data: distributions } = useDistributionsQuery(deviceId ? Number(deviceId) : undefined, { enabled: open });
  const { mutateAsync: createMachine, isPending: isSaving } = useCreateMachineMutation({
    onSuccess: (created) => openOperationViewer(created.operationId),
  });
  const selectedDistribution = distributions?.find((item) => item.key === distribution);

  useEffect(() => {
    if (open) {
      reset({ ...DEFAULT_MACHINE_FORM_VALUES, deviceId: device ? String(device.id) : "" });
    }
  }, [open, device, reset]);

  useEffect(() => {
    if (selectedDistribution && !selectedDistribution.versions.includes(watch("version"))) {
      setValue("version", selectedDistribution.versions[0]);
    }
  }, [selectedDistribution, setValue, watch]);

  const onSubmit = async (values: MachineFormValues) => {
    await createMachine({
      deviceId: Number(values.deviceId),
      name: values.name,
      distribution: values.distribution,
      version: values.version,
      cpuLimit: toOptionalNumber(values.cpuLimit),
      memoryLimitMb: toOptionalNumber(values.memoryLimitMb),
      networkMode: values.networkMode,
      ports: values.networkMode === "BRIDGE" ? values.ports : [],
      volumes: values.volumes,
      username: values.username,
      password: values.password,
      installSsh: values.installSsh,
      sshPort: values.installSsh ? toOptionalNumber(values.sshPort) : null,
      autoStart: values.autoStart,
    });
    onOpenChange(false);
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title="Nova máquina Linux"
      description="Um sistema Linux completo rodando em contêiner Docker, com usuário, sudo e SSH próprios."
      className="max-w-2xl"
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form="machine-form" loading={isSaving}>
            Criar máquina
          </Button>
        </>
      }
    >
      <form id="machine-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6">
        <section className="flex flex-col gap-4">
          <Typography variant="section-label">Sistema</Typography>
          {!device && (
            <FieldWrapper label="Dispositivo" htmlFor="machine-device" error={errors.deviceId?.message}>
              <Select id="machine-device" {...register("deviceId")} aria-invalid={Boolean(errors.deviceId)}>
                <option value="">Escolha</option>
                {devices?.data.map((item) => (
                  <option key={item.id} value={String(item.id)} disabled={!item.online}>
                    {item.name} ({item.architecture ?? "?"}){item.online ? "" : " — desconectado"}
                  </option>
                ))}
              </Select>
            </FieldWrapper>
          )}
          <FieldWrapper label="Nome" htmlFor="machine-name" error={errors.name?.message} description="Vira também o nome do host dentro da máquina">
            <Input id="machine-name" placeholder="web-teste" {...register("name")} aria-invalid={Boolean(errors.name)} />
          </FieldWrapper>
          <div className="grid gap-4 sm:grid-cols-2">
            <FieldWrapper label="Distribuição" htmlFor="machine-distribution">
              <Select id="machine-distribution" {...register("distribution")}>
                {(distributions ?? []).map((item) => (
                  <option key={item.key} value={item.key} disabled={!item.supported}>
                    {item.name}
                    {item.supported ? "" : " (sem imagem para este processador)"}
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
        </section>

        <section className="flex flex-col gap-4">
          <Typography variant="section-label">Recursos</Typography>
          <div className="grid gap-4 sm:grid-cols-2">
            <FieldWrapper label="Limite de CPUs" htmlFor="machine-cpu" error={errors.cpuLimit?.message} description="Vazio = sem limite. Ex.: 0.5, 1, 2">
              <Input id="machine-cpu" type="number" step="0.1" inputMode="decimal" {...register("cpuLimit")} />
            </FieldWrapper>
            <FieldWrapper label="Limite de memória (MB)" htmlFor="machine-memory" error={errors.memoryLimitMb?.message} description="Vazio = sem limite">
              <Input id="machine-memory" type="number" inputMode="numeric" {...register("memoryLimitMb")} />
            </FieldWrapper>
          </div>
        </section>

        <section className="flex flex-col gap-4">
          <Typography variant="section-label">Rede</Typography>
          <FieldWrapper
            label="Tipo de rede"
            htmlFor="machine-network"
            description={
              networkMode === "HOST"
                ? "Usa a rede do dispositivo: os serviços da máquina respondem direto no IP dele. Obrigatório quando a internet do dispositivo vem por proxy local, como no celular."
                : "Rede isolada: só as portas encaminhadas abaixo ficam acessíveis pelo IP do dispositivo."
            }
          >
            <Select id="machine-network" {...register("networkMode")}>
              <option value="HOST">Rede do dispositivo</option>
              <option value="BRIDGE">Isolada, com portas encaminhadas</option>
            </Select>
          </FieldWrapper>
          {networkMode === "BRIDGE" && (
            <div className="flex flex-col gap-2">
              {ports.fields.map((field, index) => (
                <div key={field.id} className="grid grid-cols-[1fr_1fr_6rem_auto] items-end gap-2">
                  <FieldWrapper label="Porta no dispositivo" htmlFor={`port-host-${index}`} error={errors.ports?.[index]?.hostPort?.message}>
                    <Input id={`port-host-${index}`} type="number" {...register(`ports.${index}.hostPort`)} />
                  </FieldWrapper>
                  <FieldWrapper label="Porta na máquina" htmlFor={`port-container-${index}`} error={errors.ports?.[index]?.containerPort?.message}>
                    <Input id={`port-container-${index}`} type="number" {...register(`ports.${index}.containerPort`)} />
                  </FieldWrapper>
                  <FieldWrapper label="Protocolo" htmlFor={`port-protocol-${index}`}>
                    <Select id={`port-protocol-${index}`} {...register(`ports.${index}.protocol`)}>
                      <option value="tcp">tcp</option>
                      <option value="udp">udp</option>
                    </Select>
                  </FieldWrapper>
                  <Button variant="ghost" size="icon" onClick={() => ports.remove(index)} aria-label="Remover porta">
                    <Trash2 />
                  </Button>
                </div>
              ))}
              <Button variant="outline" size="sm" className="self-start" onClick={() => ports.append({ hostPort: 8080, containerPort: 80, protocol: "tcp" })}>
                <Plus />
                Encaminhar porta
              </Button>
            </div>
          )}
        </section>

        <section className="flex flex-col gap-4">
          <Typography variant="section-label">Pastas compartilhadas</Typography>
          {volumes.fields.map((field, index) => (
            <div key={field.id} className="grid grid-cols-[1fr_1fr_auto] items-end gap-2">
              <FieldWrapper label="Pasta no dispositivo" htmlFor={`volume-host-${index}`} error={errors.volumes?.[index]?.hostPath?.message}>
                <Input id={`volume-host-${index}`} className="font-mono" {...register(`volumes.${index}.hostPath`)} />
              </FieldWrapper>
              <FieldWrapper label="Na máquina" htmlFor={`volume-container-${index}`} error={errors.volumes?.[index]?.containerPath?.message}>
                <Input id={`volume-container-${index}`} className="font-mono" {...register(`volumes.${index}.containerPath`)} />
              </FieldWrapper>
              <Button variant="ghost" size="icon" onClick={() => volumes.remove(index)} aria-label="Remover pasta">
                <Trash2 />
              </Button>
            </div>
          ))}
          <Button variant="outline" size="sm" className="self-start" onClick={() => volumes.append({ hostPath: "/srv/dados", containerPath: "/dados" })}>
            <Plus />
            Compartilhar pasta
          </Button>
        </section>

        <section className="flex flex-col gap-4">
          <Typography variant="section-label">Acesso</Typography>
          <div className="grid gap-4 sm:grid-cols-2">
            <FieldWrapper label="Usuário" htmlFor="machine-user" error={errors.username?.message}>
              <Input id="machine-user" autoComplete="off" {...register("username")} aria-invalid={Boolean(errors.username)} />
            </FieldWrapper>
            <FieldWrapper label="Senha (também do sudo)" htmlFor="machine-password" error={errors.password?.message}>
              <Input id="machine-password" type="password" autoComplete="new-password" {...register("password")} aria-invalid={Boolean(errors.password)} />
            </FieldWrapper>
          </div>
          <SwitchRow
            id="machine-ssh"
            title="Instalar SSH"
            description="Permite entrar na máquina direto, sem passar pelo painel."
            checked={installSsh}
            onCheckedChange={(value) => setValue("installSsh", value)}
          />
          {installSsh && (
            <FieldWrapper label="Porta do SSH" htmlFor="machine-ssh-port" error={errors.sshPort?.message} className="max-w-xs">
              <Input id="machine-ssh-port" type="number" inputMode="numeric" {...register("sshPort")} />
            </FieldWrapper>
          )}
          <SwitchRow
            id="machine-autostart"
            title="Ligar junto com o dispositivo"
            description="A máquina volta sozinha depois que o dispositivo reinicia."
            checked={watch("autoStart")}
            onCheckedChange={(value) => setValue("autoStart", value)}
          />
        </section>
      </form>
    </AppSheet>
  );
};

