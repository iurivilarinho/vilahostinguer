import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Dialog, LogViewer, Skeleton, Typography } from "@/components";
import { serverKeys, useServerOperationQuery } from "../api";

type OperationDialogProps = {
  serverId: number;
  operationId: number | null;
  onClose: () => void;
};

/** Acompanha ao vivo uma tarefa do servidor (ligar, reinstalar, restaurar, backup). */
export const OperationDialog = ({ serverId, operationId, onClose }: OperationDialogProps) => {
  const queryClient = useQueryClient();
  const { data: operation, isLoading } = useServerOperationQuery(serverId, operationId);

  useEffect(() => {
    if (operation?.finished) {
      queryClient.invalidateQueries({ queryKey: serverKeys.all });
    }
  }, [operation?.finished, queryClient]);

  const icon = !operation || !operation.finished ? (
    <Loader2 className="size-5 animate-spin text-primary" />
  ) : operation.status === "SUCCEEDED" ? (
    <CheckCircle2 className="size-5 text-success-foreground" />
  ) : (
    <XCircle className="size-5 text-destructive-foreground" />
  );

  return (
    <Dialog
      open={operationId !== null}
      onOpenChange={(open) => !open && onClose()}
      title={operation?.title ?? "Tarefa"}
      description="Você pode fechar esta janela: a tarefa continua no servidor."
      className="max-w-3xl"
      footer={
        <Button variant="outline" onClick={onClose}>
          Fechar
        </Button>
      }
    >
      {isLoading || !operation ? (
        <Skeleton className="h-64 w-full" />
      ) : (
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-2">
            {icon}
            <Typography variant="ui-header">{operation.statusDescription}</Typography>
          </div>
          <LogViewer text={operation.log || "Começando..."} />
        </div>
      )}
    </Dialog>
  );
};
