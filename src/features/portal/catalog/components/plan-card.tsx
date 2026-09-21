import { Archive, Check, Cpu, HardDrive, MemoryStick } from "lucide-react";
import type { ReactNode } from "react";
import { Badge, Button, Card, Typography } from "@/components";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/merge-classes";
import type { BillingCycle, PortalPlanDto } from "../api";

type PlanCardProps = {
  plan: PortalPlanDto;
  cycle: BillingCycle;
  onChoose: (plan: PortalPlanDto) => void;
};

const Spec = ({ icon, children }: { icon: ReactNode; children: ReactNode }) => (
  <li className="flex items-center gap-3 [&>svg]:size-4 [&>svg]:shrink-0 [&>svg]:text-primary">
    {icon}
    <Typography variant="body-sm" as="span">
      {children}
    </Typography>
  </li>
);

const memoryLabel = (memoryMb: number) => (memoryMb >= 1024 ? `${(memoryMb / 1024).toLocaleString("pt-BR")} GB de RAM` : `${memoryMb} MB de RAM`);

/** Plano na vitrine: preço por mês no período escolhido, recursos e o botão de contratar. */
export const PlanCard = ({ plan, cycle, onChoose }: PlanCardProps) => {
  const price = plan.prices.find((item) => item.cycle === cycle) ?? plan.prices[0];
  const monthly = plan.prices.find((item) => item.cycle === "MONTHLY");

  return (
    <Card className={cn("relative flex flex-col gap-5 p-6", plan.featured && "border-2 border-primary shadow-lg")}>
      {plan.featured && (
        <span className="absolute -top-3 left-6 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground">MAIS POPULAR</span>
      )}
      <div className="flex flex-col gap-1">
        <Typography variant="title-lg" as="h3">
          {plan.name}
        </Typography>
        {plan.description && (
          <Typography variant="body-sm" className="text-muted-foreground">
            {plan.description}
          </Typography>
        )}
      </div>
      <div className="flex flex-col gap-1">
        {price.discountPercent > 0 && monthly && (
          <div className="flex items-center gap-2">
            <Typography variant="body-sm" as="span" className="text-muted-foreground line-through">
              {formatCurrency(monthly.perMonth)}
            </Typography>
            <Badge tone="success">Economize {price.discountPercent}%</Badge>
          </div>
        )}
        <div className="flex items-baseline gap-1">
          <Typography variant="display-md" as="span">
            {formatCurrency(price.perMonth)}
          </Typography>
          <Typography variant="body-sm" as="span" className="text-muted-foreground">
            /mês
          </Typography>
        </div>
        <Typography variant="caption">
          {price.months === 1 ? "Cobrado todo mês" : `${formatCurrency(price.total)} a cada ${price.months} meses`}
        </Typography>
      </div>
      <Button size="lg" variant={plan.featured ? "primary" : "secondary"} disabled={!plan.available} onClick={() => onChoose(plan)}>
        {plan.available ? "Contratar" : "Esgotado"}
      </Button>
      <ul className="flex flex-col gap-3">
        <Spec icon={<Cpu />}>
          {plan.cpuLimit.toLocaleString("pt-BR")} {plan.cpuLimit === 1 ? "núcleo de CPU" : "núcleos de CPU"}
        </Spec>
        <Spec icon={<MemoryStick />}>{memoryLabel(plan.memoryMb)}</Spec>
        <Spec icon={<HardDrive />}>{plan.diskGb} GB de disco</Spec>
        <Spec icon={<Archive />}>{plan.backupSlots === 0 ? "Sem backups" : `${plan.backupSlots} backups guardados`}</Spec>
        <Spec icon={<Check />}>Acesso root (sudo) e SSH</Spec>
        <Spec icon={<Check />}>Terminal no navegador</Spec>
      </ul>
      <div className="flex flex-wrap gap-1.5 border-t border-border pt-4">
        {plan.distributions.map((distribution) => (
          <Badge key={distribution.key}>{distribution.name}</Badge>
        ))}
      </div>
    </Card>
  );
};
