import { ArrowLeft, CheckCircle2 } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { Card, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format";
import { usePortalInfoQuery } from "../../catalog/api";
import { useCheckInvoiceMutation, useInvoiceQuery, usePayInvoiceMutation } from "../api";
import { PixPayment } from "../components/pix-payment";
import { InvoiceStatusBadge } from "../components/status-badges";

export const PageInvoice = () => {
  const { id } = useParams<{ id: string }>();
  const invoiceId = Number(id);
  const { data: invoice, isLoading, isError, error, refetch, isFetching } = useInvoiceQuery(Number.isFinite(invoiceId) ? invoiceId : undefined);
  const { data: info } = usePortalInfoQuery();
  const { mutate: pay, isPending: isGenerating } = usePayInvoiceMutation();
  const { mutate: check, isPending: isChecking } = useCheckInvoiceMutation();

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar a fatura.")} onRetry={() => refetch()} retrying={isFetching} />;
  }
  if (isLoading || !invoice) {
    return <Skeleton className="h-80 w-full" />;
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6">
      <Link to={RotasPortal.billing} className="inline-link flex items-center gap-1 text-sm">
        <ArrowLeft className="size-4" />
        Faturamento
      </Link>
      <Card className="flex flex-col gap-4 p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="flex flex-col gap-1">
            <Typography variant="caption">Fatura nº {invoice.id}</Typography>
            <Typography variant="title-md">{invoice.description}</Typography>
          </div>
          <InvoiceStatusBadge status={invoice.status} label={invoice.statusDescription} overdue={invoice.overdue} />
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="flex flex-col">
            <Typography variant="caption">Valor</Typography>
            <Typography variant="title-lg" as="span">
              {formatCurrency(invoice.amount)}
            </Typography>
          </div>
          <div className="flex flex-col">
            <Typography variant="caption">Vencimento</Typography>
            <Typography variant="body-md">{formatDate(invoice.dueDate)}</Typography>
          </div>
          <div className="flex flex-col">
            <Typography variant="caption">Emissão</Typography>
            <Typography variant="body-md">{formatDateTime(invoice.createdAt)}</Typography>
          </div>
        </div>
      </Card>

      {invoice.status === "PAID" && (
        <Card className="flex items-center gap-4 border-success bg-success-soft p-5">
          <CheckCircle2 className="size-8 shrink-0 text-success-foreground" />
          <div className="flex flex-col gap-1">
            <Typography variant="title-sm" className="text-success-foreground">
              Pagamento confirmado
            </Typography>
            <Typography variant="body-sm" className="text-success-foreground">
              {invoice.renewal ? "Seu período foi renovado." : "Estamos criando o seu servidor; ele aparece em VPS em alguns minutos."} Pago em{" "}
              {formatDateTime(invoice.paidAt)}.
            </Typography>
            {!invoice.renewal && (
              <Link to={RotasPortal.servers} className="inline-link text-sm">
                Ir para os servidores
              </Link>
            )}
          </div>
        </Card>
      )}

      {invoice.status === "OPEN" && (
        <PixPayment
          invoice={invoice}
          automaticPix={info?.automaticPix ?? false}
          manualInstructions={info?.manualPaymentInstructions ?? null}
          generating={isGenerating}
          checking={isChecking}
          onGenerate={() => pay(invoice.id)}
          onCheck={() => check(invoice.id)}
        />
      )}

      {invoice.status === "CANCELED" && (
        <Card className="p-5">
          <Typography variant="body-sm">Esta fatura foi cancelada{invoice.note ? `: ${invoice.note}` : "."}</Typography>
        </Card>
      )}
    </div>
  );
};
