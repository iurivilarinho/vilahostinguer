import { Ban, CreditCard, MoreHorizontal, Pause, Play, RotateCw, Search } from "lucide-react";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Badge,
  Button,
  Card,
  DropdownMenu,
  EmptyState,
  Input,
  PageHeader,
  QueryErrorState,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  Typography,
} from "@/components";
import { usePaginatedData } from "@/app/hooks/use-paginated-data";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { Rotas } from "@/app/variables/rotas";
import { SubscriptionStatusBadge } from "@/features/portal/billing/components/status-badges";
import { formatCurrency, formatDate } from "@/lib/format";
import {
  useAdminSubscriptionsQuery,
  useRetrySubscriptionMutation,
  useSubscriptionStatusMutation,
  type AdminSubscriptionFilter,
  type SubscriptionDto,
  type SubscriptionStatus,
} from "../api";
import { ReasonDialog } from "../components/reason-dialog";

type View = "vivas" | "canceladas";
type Change = { subscription: SubscriptionDto; status: SubscriptionStatus };

const LIVE: SubscriptionStatus[] = ["PENDING_PAYMENT", "PROVISIONING", "ACTIVE", "SUSPENDED"];
const SORT = [{ by: "createdAt" as const, direction: "desc" as const }];

const COPY: Partial<Record<SubscriptionStatus, { title: string; description: string; label: string }>> = {
  SUSPENDED: { title: "Suspender", label: "Suspender", description: "O servidor é desligado e as rotas pausadas. Os dados ficam." },
  ACTIVE: { title: "Reativar", label: "Reativar", description: "O servidor volta a ligar e as rotas voltam ao ar." },
  CANCELED: { title: "Cancelar", label: "Cancelar e apagar", description: "O servidor é apagado, as rotas removidas e as faturas em aberto canceladas." },
};

export const PageSubscriptions = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const customerId = searchParams.get("cliente") ? Number(searchParams.get("cliente")) : undefined;
  const [view, setView] = useState<View>("vivas");
  const [search, setSearch] = useState("");
  const [change, setChange] = useState<Change | null>(null);
  const filter: AdminSubscriptionFilter = { search: search.trim() || undefined, customerId, status: view === "vivas" ? LIVE : ["CANCELED"] };
  const { data: subscriptions, isLoading, isError, error, refetch, isFetching, pagination, updatePagination } = usePaginatedData<
    SubscriptionDto,
    AdminSubscriptionFilter
  >({ query: useAdminSubscriptionsQuery, filter, sort: SORT, storageKey: "subscriptionsPagination" });
  const { mutate: changeStatus, isPending: isChanging } = useSubscriptionStatusMutation({ onSuccess: () => setChange(null) });
  const { mutate: retry } = useRetrySubscriptionMutation();
  const copy = change ? COPY[change.status] : undefined;

  return (
    <>
      <PageHeader title="Assinaturas" description="Servidores contratados pelos clientes." />
      <div className="flex flex-wrap items-end justify-between gap-4">
        <Tabs
          value={view}
          onValueChange={setView}
          items={[
            { value: "vivas", label: "Em andamento" },
            { value: "canceladas", label: "Canceladas" },
          ]}
        />
        <div className="relative w-72">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Servidor ou cliente" className="pl-9" />
        </div>
      </div>
      {customerId && (
        <Typography variant="caption">
          Só do cliente {customerId}.{" "}
          <button type="button" className="inline-link cursor-pointer" onClick={() => navigate(Rotas.business.subscriptions)}>
            Ver todas
          </button>
        </Typography>
      )}
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as assinaturas.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : (
        <Card>
          {isLoading ? (
            <div className="p-5">
              <Skeleton className="h-24 w-full" />
            </div>
          ) : subscriptions.length === 0 ? (
            <EmptyState icon={<CreditCard />} title="Nenhuma assinatura" description="As contratações feitas no painel do cliente aparecem aqui." />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Servidor</TableHead>
                    <TableHead>Cliente</TableHead>
                    <TableHead>Plano</TableHead>
                    <TableHead>Vencimento</TableHead>
                    <TableHead>Situação</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {subscriptions.map((subscription) => (
                    <TableRow key={subscription.id}>
                      <TableCell>
                        <Typography variant="body-sm" className="font-medium">
                          {subscription.hostname}
                        </Typography>
                        <Typography variant="caption">
                          {subscription.distributionName} {subscription.version}
                          {subscription.machine ? ` · máquina ${subscription.machine.name}` : ""}
                          {subscription.sshPort ? ` · SSH ${subscription.sshPort}` : ""}
                        </Typography>
                        {subscription.provisionError && (
                          <Typography variant="caption" as="p" className="text-destructive-foreground">
                            {subscription.provisionError}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{subscription.customer.name}</Typography>
                        <Typography variant="caption">{subscription.customer.email}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">
                          {subscription.planName} · {subscription.cycleDescription}
                        </Typography>
                        <Typography variant="caption">{formatCurrency(subscription.price)}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{formatDate(subscription.nextDueDate)}</Typography>
                        {subscription.cancelAtPeriodEnd && <Badge tone="warning">Cancela no fim</Badge>}
                      </TableCell>
                      <TableCell>
                        <SubscriptionStatusBadge status={subscription.status} label={subscription.statusDescription} />
                      </TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={
                            <Button variant="ghost" size="icon" aria-label={`Ações de ${subscription.hostname}`}>
                              <MoreHorizontal />
                            </Button>
                          }
                          items={[
                            {
                              label: "Faturas",
                              icon: <CreditCard />,
                              onSelect: () => navigate(`${Rotas.business.invoices}?assinatura=${subscription.id}`),
                            },
                            { label: "Tentar criar de novo", icon: <RotateCw />, disabled: subscription.status !== "PROVISIONING", onSelect: () => retry(subscription.id) },
                            subscription.status === "SUSPENDED"
                              ? { label: "Reativar", icon: <Play />, onSelect: () => setChange({ subscription, status: "ACTIVE" }) }
                              : {
                                  label: "Suspender",
                                  icon: <Pause />,
                                  disabled: subscription.status !== "ACTIVE",
                                  onSelect: () => setChange({ subscription, status: "SUSPENDED" }),
                                },
                            {
                              label: "Cancelar",
                              icon: <Ban />,
                              destructive: true,
                              disabled: subscription.status === "CANCELED",
                              onSelect: () => setChange({ subscription, status: "CANCELED" }),
                            },
                          ]}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <TableFooter pagination={pagination} onPageChange={(page) => updatePagination({ page })} onSizeChange={(size) => updatePagination({ size })} />
            </>
          )}
        </Card>
      )}
      <ReasonDialog
        open={change !== null}
        title={change && copy ? `${copy.title} ${change.subscription.hostname}?` : ""}
        description={copy?.description ?? ""}
        confirmLabel={copy?.label ?? "Confirmar"}
        destructive={change?.status !== "ACTIVE"}
        loading={isChanging}
        typeToConfirm={change?.status === "CANCELED" ? change.subscription.hostname : undefined}
        onClose={() => setChange(null)}
        onConfirm={(reason) => change && changeStatus({ id: change.subscription.id, status: change.status, reason })}
      />
    </>
  );
};
