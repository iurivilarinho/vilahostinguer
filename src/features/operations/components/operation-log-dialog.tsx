import { Square } from "lucide-react";
import { Button, Dialog, LogViewer, Skeleton, Typography } from "@/components";
import { formatDateTime, formatDuration } from "@/lib/format";
import { useCancelOperationMutation, useOperationQuery } from "../api";
import { useOperationViewerStore } from "../model";
import { OperationStatusBadge } from "./operation-status-badge";

/** Saída de uma operação, ao vivo enquanto roda. Montado uma vez no layout. */
export const OperationLogDialog = () => {
  const operationId = useOperationViewerStore((state) => state.operationId);
  const close = useOperationViewerStore((state) => state.close);
  const { data: operation, isLoading } = useOperationQuery(operationId ?? undefined);
  const { mutate: cancel, isPending: isCanceling } = useCancelOperationMutation();

  return (
    <Dialog
      open={operationId !== null}
      onOpenChange={(open) => !open && close()}
      title={operation?.title ?? "Operação"}
      description={operation ? `${operation.device.name} · ${operation.typeDescription}` : undefined}
      className="max-w-3xl"
      footer={
        <>
          {operation && !operation.finished && (
            <Button variant="destructive" onClick={() => cancel(operation.id)} loading={isCanceling}>
              <Square />
              Cancelar
            </Button>
          )}
          <Button variant="outline" onClick={close}>
            Fechar
          </Button>
        </>
      }
    >
      {isLoading || !operation ? (
        <Skeleton className="h-64 w-full" />
      ) : (
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <OperationStatusBadge status={operation.status} label={operation.statusDescription} />
            <Typography variant="caption">Início {formatDateTime(operation.startedAt ?? operation.createdAt)}</Typography>
            <Typography variant="caption">Duração {formatDuration(operation.startedAt, operation.finishedAt)}</Typography>
            {operation.exitCode !== null && <Typography variant="caption">Código de saída {operation.exitCode}</Typography>}
          </div>
          <LogViewer text={operation.log} follow={!operation.finished} />
        </div>
      )}
    </Dialog>
  );
};
