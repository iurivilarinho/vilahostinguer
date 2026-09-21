import { Archive, Download, History, MoreHorizontal, ScrollText, Trash2 } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DropdownMenu,
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
import { openOperationViewer } from "@/features/operations";
import { formatBytes, formatDateTime } from "@/lib/format";
import { backupDownloadUrl, useBackupsQuery, useDiscardBackupMutation, useRestoreBackupMutation, type BackupDto, type BackupFilter, type BackupStatus } from "../api";

const STATUS_TONE: Record<BackupStatus, "info" | "success" | "destructive" | "neutral"> = {
  CREATING: "info",
  AVAILABLE: "success",
  FAILED: "destructive",
  DISCARDED: "neutral",
};

const SORT_NEWEST = [{ by: "createdAt" as const, direction: "desc" as const }];

type BackupsTableProps = {
  filter: BackupFilter;
  showDevice?: boolean;
  storageKey: string;
  emptyAction?: ReactNode;
};

export const BackupsTable = ({ filter, showDevice = true, storageKey, emptyAction }: BackupsTableProps) => {
  const [restoring, setRestoring] = useState<BackupDto | null>(null);
  const [discarding, setDiscarding] = useState<BackupDto | null>(null);
  const { mutate: restore, isPending: isRestoring } = useRestoreBackupMutation({ onSuccess: (operation) => openOperationViewer(operation.id) });
  const { mutate: discard, isPending: isDiscarding } = useDiscardBackupMutation();
  const {
    data: backups,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
    pagination,
    updatePagination,
  } = usePaginatedData<BackupDto, BackupFilter>({ query: useBackupsQuery, filter, sort: SORT_NEWEST, storageKey });

  if (isError) {
    return <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os backups.")} onRetry={() => refetch()} retrying={isFetching} />;
  }

  return (
    <Card>
      {isLoading ? (
        <div className="flex flex-col gap-3 p-5">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
        </div>
      ) : backups.length === 0 ? (
        <EmptyState
          icon={<Archive />}
          title="Nenhum backup"
          description="Gere um backup das pastas importantes antes de mudanças grandes. O arquivo fica neste computador e pode ser restaurado com um clique."
          action={emptyAction}
        />
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Backup</TableHead>
                {showDevice && <TableHead>Dispositivo</TableHead>}
                <TableHead>Pastas</TableHead>
                <TableHead>Tamanho</TableHead>
                <TableHead>Situação</TableHead>
                <TableHead>Criado</TableHead>
                <TableHead className="w-12" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {backups.map((backup) => (
                <TableRow key={backup.id}>
                  <TableCell>
                    <Typography variant="body-sm" className="font-medium">
                      {backup.name}
                    </Typography>
                  </TableCell>
                  {showDevice && (
                    <TableCell>
                      <Link to={Rotas.devices.detail(backup.device.id)} className="inline-link text-sm">
                        {backup.device.name}
                      </Link>
                    </TableCell>
                  )}
                  <TableCell>
                    <Typography variant="mono" className="text-muted-foreground" title={backup.paths.join("\n")}>
                      {backup.paths.slice(0, 3).join(" ")}
                      {backup.paths.length > 3 ? ` +${backup.paths.length - 3}` : ""}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body-sm">{formatBytes(backup.sizeBytes)}</Typography>
                  </TableCell>
                  <TableCell>
                    <Badge tone={STATUS_TONE[backup.status]}>{backup.statusDescription}</Badge>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body-sm">{formatDateTime(backup.createdAt)}</Typography>
                  </TableCell>
                  <TableCell>
                    <DropdownMenu
                      trigger={
                        <Button variant="ghost" size="icon" aria-label={`Ações de ${backup.name}`}>
                          <MoreHorizontal />
                        </Button>
                      }
                      items={[
                        {
                          label: "Baixar .tar.gz",
                          icon: <Download />,
                          disabled: backup.status !== "AVAILABLE",
                          onSelect: () => window.open(backupDownloadUrl(backup.id), "_self"),
                        },
                        { label: "Restaurar", icon: <History />, disabled: backup.status !== "AVAILABLE", onSelect: () => setRestoring(backup) },
                        {
                          label: "Ver saída",
                          icon: <ScrollText />,
                          disabled: backup.operationId === null,
                          onSelect: () => backup.operationId !== null && openOperationViewer(backup.operationId),
                        },
                        {
                          label: "Descartar",
                          icon: <Trash2 />,
                          destructive: true,
                          disabled: backup.status === "CREATING" || backup.status === "DISCARDED",
                          onSelect: () => setDiscarding(backup),
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

      <ConfirmDialog
        open={restoring !== null}
        onOpenChange={(open) => !open && setRestoring(null)}
        title={restoring ? `Restaurar "${restoring.name}"?` : "Restaurar"}
        description="Os arquivos do backup sobrescrevem os atuais no dispositivo. Arquivos criados depois do backup continuam lá."
        confirmLabel="Restaurar"
        loading={isRestoring}
        onConfirm={() => restoring && restore(restoring.id, { onSettled: () => setRestoring(null) })}
      />
      <ConfirmDialog
        open={discarding !== null}
        onOpenChange={(open) => !open && setDiscarding(null)}
        title={discarding ? `Descartar "${discarding.name}"?` : "Descartar"}
        description="O arquivo é apagado deste computador para liberar espaço. O registro continua no histórico."
        confirmLabel="Descartar"
        destructive
        loading={isDiscarding}
        onConfirm={() => discarding && discard(discarding.id, { onSettled: () => setDiscarding(null) })}
      />
    </Card>
  );
};
