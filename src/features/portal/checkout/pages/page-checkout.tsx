import { zodResolver } from "@hookform/resolvers/zod";
import { ShieldCheck } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { Badge, Button, Card, FieldWrapper, Input, QueryErrorState, Select, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { formatCurrency } from "@/lib/format";
import { useCheckoutMutation } from "../../billing/api";
import { usePlansQuery, type BillingCycle } from "../../catalog/api";
import { OptionCard } from "../components/option-card";
import { checkoutSchema, DEFAULT_CHECKOUT_VALUES, type CheckoutFormValues } from "../form/schema";

const CYCLES: BillingCycle[] = ["MONTHLY", "QUARTERLY", "SEMIANNUAL", "ANNUAL"];

const isCycle = (value: string | null): value is BillingCycle => CYCLES.includes(value as BillingCycle);

/** Carrinho: período, sistema, nome do servidor e acesso; o resumo fica fixo ao lado. */
export const PageCheckout = () => {
  const navigate = useNavigate();
  const { planId } = useParams<{ planId: string }>();
  const [searchParams] = useSearchParams();
  const { data: plans, isLoading, isError, error, refetch, isFetching } = usePlansQuery();
  const plan = plans?.find((item) => String(item.id) === planId);
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<CheckoutFormValues>({ resolver: zodResolver(checkoutSchema), defaultValues: DEFAULT_CHECKOUT_VALUES });
  const { mutate: checkout, isPending } = useCheckoutMutation({ onSuccess: (created) => navigate(RotasPortal.invoice(created.invoice.id)) });
  const cycle = watch("cycle");
  const distribution = watch("distribution");
  const selectedDistribution = plan?.distributions.find((item) => item.key === distribution);
  const price = plan?.prices.find((item) => item.cycle === cycle);

  useEffect(() => {
    const requested = searchParams.get("periodo");
    if (isCycle(requested)) {
      setValue("cycle", requested);
    }
  }, [searchParams, setValue]);

  useEffect(() => {
    if (plan && !plan.distributions.some((item) => item.key === distribution)) {
      setValue("distribution", plan.distributions[0]?.key ?? "UBUNTU");
    }
  }, [plan, distribution, setValue]);

  useEffect(() => {
    if (selectedDistribution && !selectedDistribution.versions.includes(watch("version"))) {
      setValue("version", selectedDistribution.versions[0]);
    }
  }, [selectedDistribution, setValue, watch]);

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar o plano.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  if (isLoading) {
    return <Skeleton className="h-96 w-full" />;
  }
  if (!plan) {
    return (
      <Card className="flex flex-col items-start gap-3 p-6">
        <Typography variant="title-md">Plano não encontrado</Typography>
        <Link to={`${RotasPortal.home}#planos`} className="inline-link">
          Ver os planos à venda
        </Link>
      </Card>
    );
  }

  return (
    <form
      className="grid gap-6 lg:grid-cols-[1fr_22rem]"
      onSubmit={handleSubmit((values) =>
        checkout({
          planId: plan.id,
          cycle: values.cycle,
          hostname: values.hostname,
          distribution: values.distribution,
          version: values.version,
          username: values.username,
          password: values.password,
        }),
      )}
    >
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-1">
          <Typography variant="display-sm">Contratar {plan.name}</Typography>
          <Typography variant="body-sm" className="text-muted-foreground">
            O servidor é criado assim que a primeira fatura for paga.
          </Typography>
        </div>

        <Card className="flex flex-col gap-4 p-5">
          <Typography variant="title-sm">1. Período</Typography>
          <div role="radiogroup" className="grid gap-3 sm:grid-cols-2">
            {plan.prices.map((option) => (
              <OptionCard key={option.cycle} selected={cycle === option.cycle} onSelect={() => setValue("cycle", option.cycle)}>
                <div className="flex items-center justify-between gap-2">
                  <Typography variant="ui-header">{option.cycleDescription}</Typography>
                  {option.discountPercent > 0 && <Badge tone="success">-{option.discountPercent}%</Badge>}
                </div>
                <Typography variant="title-md" as="span">
                  {formatCurrency(option.perMonth)}
                  <span className="text-sm font-normal text-muted-foreground">/mês</span>
                </Typography>
                <Typography variant="caption">{formatCurrency(option.total)} por período</Typography>
              </OptionCard>
            ))}
          </div>
        </Card>

        <Card className="flex flex-col gap-4 p-5">
          <Typography variant="title-sm">2. Sistema operacional</Typography>
          <div role="radiogroup" className="grid gap-3 sm:grid-cols-3">
            {plan.distributions.map((option) => (
              <OptionCard key={option.key} selected={distribution === option.key} onSelect={() => setValue("distribution", option.key)}>
                <Typography variant="ui-header">{option.name}</Typography>
                <Typography variant="caption">{option.versions.join(" · ")}</Typography>
              </OptionCard>
            ))}
          </div>
          <FieldWrapper label="Versão" htmlFor="checkout-version" error={errors.version?.message} className="max-w-xs">
            <Select id="checkout-version" {...register("version")}>
              {(selectedDistribution?.versions ?? []).map((version) => (
                <option key={version} value={version}>
                  {version}
                </option>
              ))}
            </Select>
          </FieldWrapper>
        </Card>

        <Card className="flex flex-col gap-4 p-5">
          <Typography variant="title-sm">3. Servidor e acesso</Typography>
          <FieldWrapper label="Nome do servidor" htmlFor="checkout-hostname" error={errors.hostname?.message} description="Aparece no painel e vira o nome da máquina.">
            <Input id="checkout-hostname" placeholder="meu-site" autoComplete="off" {...register("hostname")} aria-invalid={Boolean(errors.hostname)} />
          </FieldWrapper>
          <FieldWrapper label="Usuário" htmlFor="checkout-username" error={errors.username?.message} description="Usuário Linux com sudo, para o SSH.">
            <Input id="checkout-username" placeholder="admin" autoComplete="off" {...register("username")} aria-invalid={Boolean(errors.username)} />
          </FieldWrapper>
          <div className="grid gap-4 sm:grid-cols-2">
            <FieldWrapper label="Senha" htmlFor="checkout-password" error={errors.password?.message}>
              <Input id="checkout-password" type="password" autoComplete="new-password" {...register("password")} aria-invalid={Boolean(errors.password)} />
            </FieldWrapper>
            <FieldWrapper label="Repita a senha" htmlFor="checkout-confirmation" error={errors.confirmation?.message}>
              <Input id="checkout-confirmation" type="password" autoComplete="new-password" {...register("confirmation")} />
            </FieldWrapper>
          </div>
          <Typography variant="caption" as="p">
            O servidor fica na internet: use uma senha longa, que você não usa em outro lugar.
          </Typography>
        </Card>
      </div>

      <aside className="lg:sticky lg:top-6 lg:self-start">
        <Card className="flex flex-col gap-4 p-5">
          <Typography variant="title-sm">Resumo do pedido</Typography>
          <div className="flex flex-col gap-2">
            <div className="flex justify-between gap-2">
              <Typography variant="body-sm">{plan.name}</Typography>
              <Typography variant="body-sm">{price?.cycleDescription}</Typography>
            </div>
            <Typography variant="caption">
              {plan.cpuLimit} CPU · {plan.memoryMb} MB RAM · {plan.diskGb} GB · {selectedDistribution?.name} {watch("version")}
            </Typography>
          </div>
          <div className="flex items-baseline justify-between border-t border-border pt-4">
            <Typography variant="ui-header">Total hoje</Typography>
            <Typography variant="title-lg" as="span">
              {formatCurrency(price?.total)}
            </Typography>
          </div>
          <Button type="submit" size="lg" loading={isPending} disabled={!plan.available}>
            {plan.available ? "Contratar e pagar" : "Plano esgotado"}
          </Button>
          <div className="flex items-start gap-2 text-muted-foreground">
            <ShieldCheck className="size-4 shrink-0" />
            <Typography variant="caption" as="p">
              Pagamento por Pix. Renovação automática por fatura; cancele quando quiser.
            </Typography>
          </div>
        </Card>
      </aside>
    </form>
  );
};
