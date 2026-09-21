import { Archive, ShieldCheck, SquareTerminal, Zap } from "lucide-react";
import type { ReactNode } from "react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, EmptyState, QueryErrorState, Skeleton, Typography, buttonVariants } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { usePlansQuery, usePortalInfoQuery, type BillingCycle, type PortalPlanDto } from "../api";
import { CycleSelector } from "../components/cycle-selector";
import { PlanCard } from "../components/plan-card";

const FEATURES: { icon: ReactNode; title: string; text: string }[] = [
  { icon: <Zap />, title: "No ar em minutos", text: "Pague com Pix e o servidor é criado sozinho, com o sistema que você escolheu." },
  { icon: <SquareTerminal />, title: "Acesso total", text: "Usuário com sudo, SSH próprio e terminal direto no navegador." },
  { icon: <Archive />, title: "Backups com um clique", text: "Guarde o servidor inteiro e volte a qualquer backup quando precisar." },
  { icon: <ShieldCheck />, title: "Isolado de verdade", text: "Cada servidor tem rede, portas e recursos só dele." },
];

const QUESTIONS: { question: string; answer: string }[] = [
  { question: "Quais sistemas posso usar?", answer: "Ubuntu, Debian, Rocky Linux e AlmaLinux, em mais de uma versão. Dá para trocar depois, reinstalando pelo painel." },
  { question: "Como acesso o servidor?", answer: "Por SSH, com o usuário e a senha que você define na contratação, ou pelo terminal do próprio painel." },
  { question: "E se eu atrasar o pagamento?", answer: "O servidor é desligado alguns dias depois do vencimento e volta assim que a fatura é paga. Depois de mais tempo sem pagamento, ele é apagado." },
  { question: "Posso cancelar quando quiser?", answer: "Sim. O cancelamento vale no fim do período pago, ou na hora, se preferir." },
];

export const PageHome = () => {
  const navigate = useNavigate();
  const [cycle, setCycle] = useState<BillingCycle>("ANNUAL");
  const { data: info } = usePortalInfoQuery();
  const { data: plans, isLoading, isError, error, refetch, isFetching } = usePlansQuery();

  const choose = (plan: PortalPlanDto) => navigate(`${RotasPortal.checkout(plan.id)}?periodo=${cycle}`);

  return (
    <>
      <section className="bg-gradient-to-b from-primary-soft/60 to-background">
        <div className="mx-auto flex w-full max-w-6xl flex-col items-center gap-5 px-4 pt-16 pb-10 text-center md:pt-24">
          <Typography variant="hero-title" className="max-w-3xl">
            Servidor VPS Linux com acesso root, pronto em minutos
          </Typography>
          <Typography variant="hero-description" className="max-w-2xl text-muted-foreground">
            Escolha o plano, o sistema e pague com Pix. {info?.companyName ?? "Nós"} cuida{info?.companyName ? "" : "amos"} do resto: rede, backups e
            um painel completo para você gerenciar tudo.
          </Typography>
          <a href="#planos" className={buttonVariants({ size: "lg" })}>
            Ver planos
          </a>
        </div>
      </section>

      <section id="planos" className="mx-auto flex w-full max-w-6xl scroll-mt-20 flex-col items-center gap-8 px-4 py-12">
        <div className="flex flex-col items-center gap-3 text-center">
          <Typography variant="section-heading">Planos de servidor VPS</Typography>
          <Typography variant="section-intro" className="text-muted-foreground">
            Quanto maior o período, menor o preço por mês.
          </Typography>
        </div>
        <CycleSelector value={cycle} onChange={setCycle} />
        {isError ? (
          <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os planos.")} onRetry={() => refetch()} retrying={isFetching} />
        ) : isLoading ? (
          <div className="grid w-full gap-6 md:grid-cols-3">
            <Skeleton className="h-[34rem]" />
            <Skeleton className="h-[34rem]" />
            <Skeleton className="h-[34rem]" />
          </div>
        ) : !plans || plans.length === 0 ? (
          <Card className="w-full">
            <EmptyState icon={<Zap />} title="Nenhum plano à venda agora" description="Volte em breve: estamos preparando novos servidores." />
          </Card>
        ) : (
          <div className="grid w-full gap-6 pt-3 md:grid-cols-2 lg:grid-cols-3">
            {plans.map((plan) => (
              <PlanCard key={plan.id} plan={plan} cycle={cycle} onChoose={choose} />
            ))}
          </div>
        )}
      </section>

      <section className="mx-auto grid w-full max-w-6xl gap-4 px-4 py-12 sm:grid-cols-2 lg:grid-cols-4">
        {FEATURES.map((feature) => (
          <Card key={feature.title} className="flex flex-col gap-3 p-6">
            <span className="flex size-11 items-center justify-center rounded-lg bg-primary-soft text-primary [&>svg]:size-5">{feature.icon}</span>
            <Typography variant="title-sm" as="h3">
              {feature.title}
            </Typography>
            <Typography variant="body-sm" className="text-muted-foreground">
              {feature.text}
            </Typography>
          </Card>
        ))}
      </section>

      <section className="mx-auto flex w-full max-w-3xl flex-col gap-4 px-4 pt-4 pb-16">
        <Typography variant="section-heading" className="text-center">
          Perguntas frequentes
        </Typography>
        {QUESTIONS.map((item) => (
          <details key={item.question} className="group rounded-lg border border-border bg-card p-5 open:shadow-card">
            <summary className="cursor-pointer list-none font-semibold text-heading">{item.question}</summary>
            <Typography variant="body-sm" as="p" className="pt-3 text-muted-foreground">
              {item.answer}
            </Typography>
          </details>
        ))}
      </section>
    </>
  );
};
