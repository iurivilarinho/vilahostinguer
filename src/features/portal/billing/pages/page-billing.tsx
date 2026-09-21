import { CreditCard, Receipt } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import {
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
  Tabs,
  Typography,
  buttonVariants,
} from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { formatCurrency, formatDate } from "@/lib/format";
import { useInvoicesQuery, useKeepSubscriptionMutation, useSubscriptionsQuery } from "../api";
import { InvoiceStatusBadge, SubscriptionStatusBadge } from "../components/status-badges";

type BillingTab = "faturas" | "assinaturas";

const PAGE = { page: 0, size: 50 };

const Invoices = () => {
  const { data, isLoading, isError, error, refetch, isFetching } = useInvoicesQuery({ ...PAGE, sort: [{ by: "createdAt", direction: "desc" }] });
  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as faturas.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  return (
    <Card>
      {isLoading ? (
        <div className="p-5">
          <Skeleton className="h-24 w-full" />
        </div>
      ) : (data?.data.length ?? 0) === 0 ? (
        <EmptyState icon={<Receipt />} title="Nenhuma fatura" description="As faturas aparecem aqui quando você contrata um servidor." />
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Fatura</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>Vencimento</TableHead>
              <TableHead>Valor</TableHead>
              <TableHead>Situação</TableHead>
              <TableHead className="w-28" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.data.map((invoice) => (
              <TableRow key={invoice.id}>
                <TableCell>
                  <Typography variant="mono">#{invoice.id}</Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body-sm">{invoice.description}</Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body-sm">{formatDate(invoice.dueDate)}</Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body-sm" className="font-semibold">
                    {formatCurrency(invoice.amount)}
                  </Typography>
                </TableCell>
                <TableCell>
                  <InvoiceStatusBadge status={invoice.status} label={invoice.statusDescription} overdue={invoice.overdue} />
                </TableCell>
                <TableCell>
                  <Link to={RotasPortal.invoice(invoice.id)} className={buttonVariants({ size: "sm", variant: invoice.status === "OPEN" ? "primary" : "outline" })}>
                    {invoice.status === "OPEN" ? "Pagar" : "Ver"}
                  </Link>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Card>
  );
};

const Subscriptions = () => {
  const { data, isLoading, isError, error, refetch, isFetching } = useSubscriptionsQuery({ ...PAGE, sort: [{ by: "createdAt", direction: "desc" }] });
  const { mutate: keep, isPending: isKeeping } = useKeepSubscriptionMutation();
  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as assinaturas.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  return (
    <Card>
      {isLoading ? (
        <div className="p-5">
          <Skeleton className="h-24 w-full" />
        </div>
      ) : (data?.data.length ?? 0) === 0 ? (
        <EmptyState
          icon={<CreditCard />}
          title="Nenhuma assinatura"
          description="Contrate um servidor para começar."
          action={
            <Link to={`${RotasPortal.home}#planos`} className={buttonVariants()}>
              Ver planos
            </Link>
          }
        />
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Servidor</TableHead>
              <TableHead>Plano</TableHead>
              <TableHead>Valor</TableHead>
              <TableHead>Próximo vencimento</TableHead>
              <TableHead>Situação</TableHead>
              <TableHead className="w-44" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.data.map((subscription) => (
              <TableRow key={subscription.id}>
                <TableCell>
                  <Typography variant="body-sm" className="font-medium">
                    {subscription.hostname}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body-sm">
                    {subscription.planName} · {subscription.cycleDescription}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body-sm">{formatCurrency(subscription.price)}</Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body-sm">
                    {subscription.cancelAtPeriodEnd ? `Encerra em ${formatDate(subscription.nextDueDate)}` : formatDate(subscription.nextDueDate)}
                  </Typography>
                </TableCell>
                <TableCell>
                  <SubscriptionStatusBadge status={subscription.status} label={subscription.statusDescription} />
                </TableCell>
                <TableCell>
                  {subscription.cancelAtPeriodEnd && subscription.status === "ACTIVE" ? (
                    <Button size="sm" variant="outline" loading={isKeeping} onClick={() => keep(subscription.id)}>
                      Manter assinatura
                    </Button>
                  ) : (
                    subscription.status !== "CANCELED" && (
                      <Link to={RotasPortal.server(subscription.id)} className={buttonVariants({ size: "sm", variant: "outline" })}>
                        Gerenciar
                      </Link>
                    )
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Card>
  );
};

export const PageBilling = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const tab: BillingTab = searchParams.get("aba") === "assinaturas" ? "assinaturas" : "faturas";
  return (
    <>
      <PageHeader title="Faturamento" description="Faturas, pagamentos e assinaturas dos seus servidores." />
      <Tabs
        value={tab}
        onValueChange={(value) => setSearchParams({ aba: value })}
        items={[
          { value: "faturas", label: "Faturas", icon: <Receipt /> },
          { value: "assinaturas", label: "Assinaturas", icon: <CreditCard /> },
        ]}
      />
      {tab === "faturas" ? <Invoices /> : <Subscriptions />}
    </>
  );
};
