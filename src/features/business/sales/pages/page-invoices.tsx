import { Ban, CheckCircle2, MoreHorizontal, Receipt } from "lucide-react";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Button,
  Card,
  DropdownMenu,
  EmptyState,
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
import { InvoiceStatusBadge } from "@/features/portal/billing/components/status-badges";
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format";
import { useAdminInvoicesQuery, useInvoiceStatusMutation, type AdminInvoiceFilter, type InvoiceDto, type InvoiceStatus } from "../api";
import { ReasonDialog } from "../components/reason-dialog";

type View = "abertas" | "vencidas" | "pagas" | "todas";
type Change = { invoice: InvoiceDto; status: InvoiceStatus };

const FILTERS: Record<View, AdminInvoiceFilter> = {
  abertas: { status: ["OPEN"] },
  vencidas: { overdue: true },
  pagas: { status: ["PAID"] },
  todas: {},
};

export const PageInvoices = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const subscriptionId = searchParams.get("assinatura") ? Number(searchParams.get("assinatura")) : undefined;
  const [view, setView] = useState<View>(subscriptionId ? "todas" : "abertas");
  const [change, setChange] = useState<Change | null>(null);
  const filter: AdminInvoiceFilter = { ...FILTERS[view], subscriptionId };
  const { data: invoices, isLoading, isError, error, refetch, isFetching, pagination, updatePagination } = usePaginatedData<InvoiceDto, AdminInvoiceFilter>({
    query: useAdminInvoicesQuery,
    filter,
    sort: [{ by: "dueDate", direction: view === "pagas" ? "desc" : "asc" }],
    storageKey: "invoicesPagination",
  });
  const { mutate: changeStatus, isPending } = useInvoiceStatusMutation({ onSuccess: () => setChange(null) });

  return (
    <>
      <PageHeader title="Faturas" description="Cobranças das assinaturas. Pagamentos por Pix automático são confirmados sozinhos." />
      <Tabs
        value={view}
        onValueChange={setView}
        items={[
          { value: "abertas", label: "Em aberto" },
          { value: "vencidas", label: "Vencidas" },
          { value: "pagas", label: "Pagas" },
          { value: "todas", label: "Todas" },
        ]}
      />
      {subscriptionId && (
        <Typography variant="caption">
          Só da assinatura {subscriptionId}.{" "}
          <button type="button" className="inline-link cursor-pointer" onClick={() => navigate(Rotas.business.invoices)}>
            Ver todas
          </button>
        </Typography>
      )}
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as faturas.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : (
        <Card>
          {isLoading ? (
            <div className="p-5">
              <Skeleton className="h-24 w-full" />
            </div>
          ) : invoices.length === 0 ? (
            <EmptyState icon={<Receipt />} title="Nenhuma fatura aqui" description="As faturas são geradas na contratação e antes de cada vencimento." />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fatura</TableHead>
                    <TableHead>Cliente</TableHead>
                    <TableHead>Vencimento</TableHead>
                    <TableHead>Valor</TableHead>
                    <TableHead>Situação</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {invoices.map((invoice) => (
                    <TableRow key={invoice.id}>
                      <TableCell>
                        <Typography variant="body-sm" className="font-medium">
                          #{invoice.id} · {invoice.description}
                        </Typography>
                        {invoice.paidAt && (
                          <Typography variant="caption">
                            {invoice.paymentMethodDescription} em {formatDateTime(invoice.paidAt)}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{invoice.customer.name}</Typography>
                        <Typography variant="caption">{invoice.customer.email}</Typography>
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
                        <DropdownMenu
                          trigger={
                            <Button variant="ghost" size="icon" aria-label={`Ações da fatura ${invoice.id}`}>
                              <MoreHorizontal />
                            </Button>
                          }
                          items={[
                            { label: "Confirmar pagamento", icon: <CheckCircle2 />, disabled: invoice.status !== "OPEN", onSelect: () => setChange({ invoice, status: "PAID" }) },
                            { label: "Cancelar fatura", icon: <Ban />, destructive: true, disabled: invoice.status !== "OPEN", onSelect: () => setChange({ invoice, status: "CANCELED" }) },
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
        title={change ? (change.status === "PAID" ? `Confirmar o pagamento da fatura ${change.invoice.id}?` : `Cancelar a fatura ${change.invoice.id}?`) : ""}
        description={
          change?.status === "PAID"
            ? `${formatCurrency(change.invoice.amount)} de ${change.invoice.customer.name}. Uma contratação nova tem o servidor criado na hora; uma renovação estende o período (e religa, se estiver suspenso).`
            : "A fatura deixa de ser cobrada. Isso não cancela a assinatura."
        }
        confirmLabel={change?.status === "PAID" ? "Confirmar pagamento" : "Cancelar fatura"}
        destructive={change?.status === "CANCELED"}
        loading={isPending}
        onClose={() => setChange(null)}
        onConfirm={(reason) => change && changeStatus({ id: change.invoice.id, status: change.status, reason })}
      />
    </>
  );
};
