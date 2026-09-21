import { History } from "lucide-react";
import { useState } from "react";
import {
  Badge,
  Card,
  EmptyState,
  PageHeader,
  QueryErrorState,
  Select,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
  Typography,
} from "@/components";
import { usePaginatedData } from "@/app/hooks/use-paginated-data";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { formatDateTime } from "@/lib/format";
import { useAuditLogsQuery, type AuditLogDto, type AuditLogFilter } from "../api";

const ENTITIES: { value: string; label: string }[] = [
  { value: "", label: "Tudo" },
  { value: "Customer", label: "Clientes" },
  { value: "Plan", label: "Planos" },
  { value: "Subscription", label: "Assinaturas e servidores" },
  { value: "Invoice", label: "Faturas" },
  { value: "PortalSettings", label: "Painel do cliente" },
];

const ENTITY_LABEL: Record<string, string> = {
  Customer: "Cliente",
  Plan: "Plano",
  Subscription: "Assinatura",
  Invoice: "Fatura",
  PortalSettings: "Painel do cliente",
};

const SORT = [{ by: "occurredAt" as const, direction: "desc" as const }];

/** Quem fez o quê: movimentações de clientes, planos, assinaturas, faturas e servidores. */
export const PageAudit = () => {
  const [entityType, setEntityType] = useState("");
  const filter: AuditLogFilter = { entityType: entityType || undefined };
  const { data: logs, isLoading, isError, error, refetch, isFetching, pagination, updatePagination } = usePaginatedData<AuditLogDto, AuditLogFilter>({
    query: useAuditLogsQuery,
    filter,
    sort: SORT,
    storageKey: "auditPagination",
  });

  return (
    <>
      <PageHeader
        title="Auditoria"
        description="Histórico imutável das movimentações feitas por clientes, pelo administrador e pelo sistema."
        actions={
          <Select value={entityType} onChange={(event) => setEntityType(event.target.value)} aria-label="Entidade" className="w-56">
            {ENTITIES.map((entity) => (
              <option key={entity.value} value={entity.value}>
                {entity.label}
              </option>
            ))}
          </Select>
        }
      />
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar a auditoria.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : (
        <Card>
          {isLoading ? (
            <div className="p-5">
              <Skeleton className="h-24 w-full" />
            </div>
          ) : logs.length === 0 ? (
            <EmptyState icon={<History />} title="Nada registrado" description="As movimentações aparecem aqui assim que acontecem." />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Quando</TableHead>
                    <TableHead>Quem</TableHead>
                    <TableHead>O quê</TableHead>
                    <TableHead>Detalhe</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {logs.map((log) => (
                    <TableRow key={log.id}>
                      <TableCell>
                        <Typography variant="body-sm">{formatDateTime(log.occurredAt)}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{log.userName ?? "—"}</Typography>
                        <Typography variant="caption">{log.source}</Typography>
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-wrap items-center gap-2">
                          <Badge tone="primary">{log.actionDescription}</Badge>
                          <Typography variant="body-sm">
                            {ENTITY_LABEL[log.entityType] ?? log.entityType} #{log.entityId}
                          </Typography>
                        </div>
                      </TableCell>
                      <TableCell className="max-w-md">
                        {log.reason && <Typography variant="body-sm">{log.reason}</Typography>}
                        {log.newValue && (
                          <Typography variant="mono" className="line-clamp-2 text-muted-foreground" title={log.newValue}>
                            {log.newValue}
                          </Typography>
                        )}
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
    </>
  );
};
