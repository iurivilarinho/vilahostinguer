import { CreditCard, Plus, Receipt, Server } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, EmptyState, Skeleton, StatCard, Typography, buttonVariants } from "@/components";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { formatCurrency, formatDate } from "@/lib/format";
import { useInvoicesQuery } from "../../billing/api";
import { InvoiceStatusBadge } from "../../billing/components/status-badges";
import { useServersQuery } from "../../servers/api";
import { ServerCard } from "../../servers/components/server-card";
import { useSessionQuery } from "../../session/api";

const OPEN_INVOICES = { page: 0, size: 5, filter: { status: ["OPEN" as const] }, sort: [{ by: "dueDate" as const, direction: "asc" as const }] };

/** Início: servidores, faturas a pagar e o atalho para contratar. */
export const PageDashboard = () => {
  const { data: customer } = useSessionQuery();
  const { data: servers, isLoading: isLoadingServers } = useServersQuery();
  const { data: invoices, isLoading: isLoadingInvoices } = useInvoicesQuery(OPEN_INVOICES);
  const running = servers?.filter((server) => server.machineStatus === "RUNNING" && server.subscriptionStatus === "ACTIVE").length ?? 0;
  const openTotal = invoices?.data.reduce((total, invoice) => total + invoice.amount, 0) ?? 0;

  return (
    <>
      <div className="flex flex-col gap-1">
        <Typography variant="display-sm">Olá, {customer?.name.split(" ")[0]}!</Typography>
        <Typography variant="body-sm" className="text-muted-foreground">
          Tudo sobre os seus servidores num só lugar.
        </Typography>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <StatCard label="Servidores" value={servers?.length ?? "—"} icon={<Server />} hint={`${running} ligado(s)`} />
        <StatCard
          label="Faturas em aberto"
          value={invoices?.totalElements ?? "—"}
          icon={<Receipt />}
          hint={openTotal > 0 ? `${formatCurrency(openTotal)} a pagar` : "Tudo em dia"}
          tone={openTotal > 0 ? "warning" : "success"}
        />
        <Link to={`${RotasPortal.home}#planos`} className="group">
          <StatCard label="Novo servidor" value="Contratar" icon={<Plus />} hint="Pronto em minutos" tone="info" className="transition-colors group-hover:border-primary" />
        </Link>
      </div>

      {(invoices?.data.length ?? 0) > 0 && (
        <Card>
          <CardHeader>
            <Typography variant="title-sm">Faturas para pagar</Typography>
            <Link to={RotasPortal.billing} className="inline-link text-sm">
              Ver todas
            </Link>
          </CardHeader>
          <CardContent className="flex flex-col divide-y divide-border p-0">
            {invoices?.data.map((invoice) => (
              <div key={invoice.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-3">
                <div className="flex flex-col">
                  <Typography variant="body-sm" className="font-medium">
                    {invoice.description}
                  </Typography>
                  <Typography variant="caption">Vence em {formatDate(invoice.dueDate)}</Typography>
                </div>
                <div className="flex items-center gap-3">
                  <InvoiceStatusBadge status={invoice.status} label={invoice.statusDescription} overdue={invoice.overdue} />
                  <Typography variant="body-sm" className="font-semibold">
                    {formatCurrency(invoice.amount)}
                  </Typography>
                  <Link to={RotasPortal.invoice(invoice.id)} className={buttonVariants({ size: "sm" })}>
                    <CreditCard />
                    Pagar
                  </Link>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <div className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <Typography variant="title-md" as="h2">
            Seus servidores
          </Typography>
          <Link to={RotasPortal.servers} className="inline-link text-sm">
            Ver todos
          </Link>
        </div>
        {isLoadingServers || isLoadingInvoices ? (
          <Skeleton className="h-28 w-full" />
        ) : (servers?.length ?? 0) === 0 ? (
          <Card>
            <EmptyState
              icon={<Server />}
              title="Você ainda não tem servidores"
              description="Escolha um plano e pague com Pix: o servidor é criado sozinho."
              action={
                <Link to={`${RotasPortal.home}#planos`} className={buttonVariants()}>
                  Ver planos
                </Link>
              }
            />
          </Card>
        ) : (
          servers?.slice(0, 5).map((server) => <ServerCard key={server.id} server={server} />)
        )}
      </div>
    </>
  );
};
