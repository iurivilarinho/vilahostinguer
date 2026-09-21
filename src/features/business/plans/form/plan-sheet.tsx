import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { AppSheet, Button, FieldWrapper, Input, Switch, Textarea, Typography } from "@/components";
import { useSavePlanMutation, type PlanDto } from "../api";
import { DEFAULT_PLAN_FORM_VALUES, planFormSchema, type PlanFormValues } from "./schema";

type PlanSheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  plan?: PlanDto;
};

const SwitchLine = ({ id, title, description, checked, onChange }: { id: string; title: string; description: string; checked: boolean; onChange: (value: boolean) => void }) => (
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

export const PlanSheet = ({ open, onOpenChange, plan }: PlanSheetProps) => {
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<PlanFormValues>({ resolver: zodResolver(planFormSchema), defaultValues: DEFAULT_PLAN_FORM_VALUES });
  const { mutateAsync: save, isPending } = useSavePlanMutation();

  useEffect(() => {
    if (!open) {
      return;
    }
    reset(
      plan
        ? {
            name: plan.name,
            description: plan.description ?? "",
            cpuLimit: plan.cpuLimit,
            memoryMb: plan.memoryMb,
            diskGb: plan.diskGb,
            backupSlots: plan.backupSlots,
            priceMonthly: plan.priceMonthly,
            active: plan.active,
            featured: plan.featured,
            orderNumber: plan.orderNumber,
          }
        : DEFAULT_PLAN_FORM_VALUES,
    );
  }, [open, plan, reset]);

  const onSubmit = async (values: PlanFormValues) => {
    await save(plan ? { id: plan.id, ...values } : values);
    onOpenChange(false);
  };

  return (
    <AppSheet
      open={open}
      onOpenChange={onOpenChange}
      title={plan ? `Editar ${plan.name}` : "Novo plano"}
      description="O que o cliente vê na vitrine. Mudar o preço vale para novas contratações; quem já contratou mantém o valor."
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isPending}>
            Cancelar
          </Button>
          <Button type="submit" form="plan-form" loading={isPending}>
            Salvar
          </Button>
        </>
      }
    >
      <form id="plan-form" onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <FieldWrapper label="Nome" htmlFor="plan-name" error={errors.name?.message}>
          <Input id="plan-name" placeholder="VPS 1" {...register("name")} />
        </FieldWrapper>
        <FieldWrapper label="Descrição" htmlFor="plan-description" error={errors.description?.message}>
          <Textarea id="plan-description" rows={2} placeholder="Para sites e APIs pequenas" {...register("description")} />
        </FieldWrapper>
        <div className="grid gap-4 sm:grid-cols-3">
          <FieldWrapper label="Processadores" htmlFor="plan-cpu" error={errors.cpuLimit?.message}>
            <Input id="plan-cpu" type="number" step="1" {...register("cpuLimit")} />
          </FieldWrapper>
          <FieldWrapper label="Memória (MB)" htmlFor="plan-memory" error={errors.memoryMb?.message}>
            <Input id="plan-memory" type="number" {...register("memoryMb")} />
          </FieldWrapper>
          <FieldWrapper label="Disco (GB)" htmlFor="plan-disk" error={errors.diskGb?.message} description="Informativo">
            <Input id="plan-disk" type="number" {...register("diskGb")} />
          </FieldWrapper>
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <FieldWrapper label="Preço mensal (R$)" htmlFor="plan-price" error={errors.priceMonthly?.message}>
            <Input id="plan-price" type="number" step="0.01" {...register("priceMonthly")} />
          </FieldWrapper>
          <FieldWrapper label="Backups guardados" htmlFor="plan-backups" error={errors.backupSlots?.message}>
            <Input id="plan-backups" type="number" {...register("backupSlots")} />
          </FieldWrapper>
          <FieldWrapper label="Ordem na vitrine" htmlFor="plan-order" error={errors.orderNumber?.message}>
            <Input id="plan-order" type="number" {...register("orderNumber")} />
          </FieldWrapper>
        </div>
        <SwitchLine id="plan-active" title="À venda" description="Aparece na vitrine do painel do cliente." checked={watch("active")} onChange={(value) => setValue("active", value)} />
        <SwitchLine
          id="plan-featured"
          title="Destaque"
          description="Ganha a faixa “Mais popular” na vitrine."
          checked={watch("featured")}
          onChange={(value) => setValue("featured", value)}
        />
      </form>
    </AppSheet>
  );
};
