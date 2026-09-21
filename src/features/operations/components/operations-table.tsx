import { Activity, ScrollText } from "lucide-react";
import { Link } from "react-router-dom";
import {
  Button,
  Card,
  EmptyState,
  QueryErrorState,
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
import { Rotas } from "@/app/variables/rotas";
import { formatDateTime, formatDuration } from "@/lib/format";
import { useOperationsQuery, type OperationBasicDto, type OperationFilter } from "../api";
import { openOperationViewer } from "../model";
import { OperationStatusBadge } from "./operation-status-badge";

const SORT_NEWEST = [{ by: "createdAt" as const, direction: "desc" as const }];
const REFRESH_MS = 5_000;

type OperationsTableProps = {
  filter: OperationFilter;
  showDevice?: boolean;
  storageKey: string;
};

/** Histórico de operações; usado na tela de Atividades e dentro de cada dispositivo. */
export const OperationsTable = ({ filter, showDevice = true, storageKey }: OperationsTableProps) => {
  const {
    data: operations,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
    pagination,
    updatePagination,
  } = usePaginatedData<OperationBasicDto, OperationFilter>({
    query: useOperationsQuery,
    filter,
    sort: SORT_NEWEST,
    storageKey,
    refetchInterval: REFRESH_MS,
  });

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as atividades.")} onRetry={() => refetch()} retrying={isFetching} />;
  }

  return (
    <Card>
      {isLoading ? (
        <div className="flex flex-col gap-3 p-5">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
        </div>
      ) : operations.length === 0 ? (
        <EmptyState
          icon={<Activity />}
          title="Nenhuma atividade"
          description="Instalações, backups, restaurações e formatações aparecem aqui, com a saída completa de cada uma."
        />
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Operação</TableHead>
                {showDevice && <TableHead>Dispositivo</TableHead>}
                <TableHead>Situação</TableHead>
                <TableHead>Início</TableHead>
                <TableHead>Duração</TableHead>
                <TableHead className="text-right">Saída</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {operations.map((operation) => (
                <TableRow key={operation.id}>
                  <TableCell>
                    <div className="flex flex-col">
                      <Typography variant="body-sm" className="font-medium">
                        {operation.title}
                      </Typography>
                      <Typography variant="caption">{operation.typeDescription}</Typography>
                    </div>
                  </TableCell>
                  {showDevice && (
                    <TableCell>
                      <Link to={Rotas.devices.detail(operation.device.id)} className="inline-link text-sm">
                        {operation.device.name}
                      </Link>
                    </TableCell>
                  )}
                  <TableCell>
                    <OperationStatusBadge status={operation.status} label={operation.statusDescription} />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body-sm">{formatDateTime(operation.startedAt ?? operation.createdAt)}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body-sm">{formatDuration(operation.startedAt, operation.finishedAt)}</Typography>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" onClick={() => openOperationViewer(operation.id)}>
                      <ScrollText />
                      Ver
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TableFooter
            pagination={pagination}
            onPageChange={(page) => updatePagination({ page })}
            onSizeChange={(size) => updatePagination({ size })}
          />
        </>
      )}
    </Card>
  );
};
