import { Package, Pencil, Plus } from "lucide-react";
import { useState } from "react";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  PageHeader,
  QueryErrorState,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Typography,
} from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { formatCurrency } from "@/lib/format";
import { usePlansAdminQuery, type PlanDto } from "../api";
import { PlanSheet } from "../form/plan-sheet";

const PARAMS = { page: 0, size: 100, sort: [{ by: "orderNumber" as const, direction: "asc" as const }] };

export const PagePlans = () => {
  const [sheetOpen, setSheetOpen] = useState(false);
  const [editing, setEditing] = useState<PlanDto | undefined>();
  const { data, isLoading, isError, error, refetch, isFetching } = usePlansAdminQuery(PARAMS);

  const open = (plan?: PlanDto) => {
    setEditing(plan);
    setSheetOpen(true);
  };

  const newPlan = (
    <Button onClick={() => open()}>
      <Plus />
      Novo plano
    </Button>
  );

  return (
    <>
      <PageHeader title="Planos" description="Servidores à venda no painel do cliente." actions={newPlan} />
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os planos.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : (
        <Card>
          {isLoading ? (
            <div className="p-5">
              <Skeleton className="h-24 w-full" />
            </div>
          ) : (data?.data.length ?? 0) === 0 ? (
            <EmptyState icon={<Package />} title="Nenhum plano" description="Crie o primeiro plano: recursos, preço e o dispositivo que hospeda os servidores." action={newPlan} />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Plano</TableHead>
                  <TableHead>Recursos</TableHead>
                  <TableHead>Mensal</TableHead>
                  <TableHead>Vendas</TableHead>
                  <TableHead>Situação</TableHead>
                  <TableHead className="w-12" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {data?.data.map((plan) => (
                  <TableRow key={plan.id}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Typography variant="body-sm" className="font-medium">
                          {plan.name}
                        </Typography>
                        {plan.featured && <Badge tone="primary">Destaque</Badge>}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body-sm">
                        {plan.cpuLimit} {plan.cpuLimit === 1 ? "processador" : "processadores"} · {plan.memoryMb} MB · {plan.diskGb} GB · {plan.backupSlots} backups
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body-sm" className="font-semibold">
                        {formatCurrency(plan.priceMonthly)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body-sm">{plan.subscriptionCount}</Typography>
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        <Badge tone={plan.active ? "success" : "neutral"}>{plan.active ? "À venda" : "Oculto"}</Badge>
                        {!plan.available && <Badge tone="warning">Esgotado</Badge>}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Button variant="ghost" size="icon" aria-label={`Editar ${plan.name}`} onClick={() => open(plan)}>
                        <Pencil />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Card>
      )}
      <PlanSheet open={sheetOpen} onOpenChange={setSheetOpen} plan={editing} />
    </>
  );
};
