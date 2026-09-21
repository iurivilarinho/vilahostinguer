import { Archive, Download, History, MoreHorizontal, Trash2 } from "lucide-react";
import { useState } from "react";
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  ConfirmDialog,
  DropdownMenu,
  EmptyState,
  Input,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Typography,
} from "@/components";
import { formatBytes, formatDateTime } from "@/lib/format";
import {
  backupFileUrl,
  useCreateServerBackupMutation,
  useDiscardServerBackupMutation,
  useRestoreServerBackupMutation,
  useServerBackupsQuery,
  type ServerBackupDto,
  type ServerBackupStatus,
  type ServerDto,
} from "../api";

const STATUS_TONE: Record<ServerBackupStatus, "info" | "success" | "destructive" | "neutral"> = {
  CREATING: "info",
  AVAILABLE: "success",
  FAILED: "destructive",
  DISCARDED: "neutral",
};

type SectionBackupsProps = {
  server: ServerDto;
  onOperation: (operationId: number) => void;
};

/** Backups do servidor inteiro: fazer agora, voltar a um backup, baixar ou apagar. */
export const SectionBackups = ({ server, onOperation }: SectionBackupsProps) => {
  const [name, setName] = useState("");
  const [restoring, setRestoring] = useState<ServerBackupDto | null>(null);
  const [discarding, setDiscarding] = useState<ServerBackupDto | null>(null);
  const active = server.subscriptionStatus === "ACTIVE";
  const { data: backups, isLoading } = useServerBackupsQuery(server.id);
  const { mutate: create, isPending: isCreating } = useCreateServerBackupMutation({
    onSuccess: (backup) => {
      setName("");
      if (backup.operationId !== null) {
        onOperation(backup.operationId);
      }
    },
  });
  const { mutate: restore, isPending: isRestoring } = useRestoreServerBackupMutation({
    onSuccess: (operation) => {
      setRestoring(null);
      onOperation(operation.id);
    },
  });
  const { mutate: discard, isPending: isDiscarding } = useDiscardServerBackupMutation({ onSuccess: () => setDiscarding(null) });
  const used = backups?.filter((backup) => backup.status === "AVAILABLE" || backup.status === "CREATING").length ?? 0;

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <div className="flex flex-col gap-1">
            <Typography variant="title-sm">Fazer backup</Typography>
            <Typography variant="caption">
              {used} de {server.backupSlots} lugares em uso. O servidor fica pausado durante a cópia, para os arquivos saírem certinhos.
            </Typography>
          </div>
        </CardHeader>
        <CardContent>
          <form
            className="flex flex-wrap gap-3"
            onSubmit={(event) => {
              event.preventDefault();
              create({ id: server.id, name });
            }}
          >
            <Input value={name} onChange={(event) => setName(event.target.value)} maxLength={120} placeholder="Nome (opcional)" className="max-w-sm flex-1" />
            <Button type="submit" loading={isCreating} disabled={!active || used >= server.backupSlots}>
              <Archive />
              Fazer backup agora
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        {isLoading ? (
          <div className="p-5">
            <Skeleton className="h-24 w-full" />
          </div>
        ) : (backups?.length ?? 0) === 0 ? (
          <EmptyState icon={<Archive />} title="Nenhum backup" description="Faça um backup antes de mudanças grandes: se algo der errado, é só voltar." />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Backup</TableHead>
                <TableHead>Tamanho</TableHead>
                <TableHead>Situação</TableHead>
                <TableHead>Data</TableHead>
                <TableHead className="w-12" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {backups?.map((backup) => (
                <TableRow key={backup.id}>
                  <TableCell>
                    <Typography variant="body-sm" className="font-medium">
                      {backup.name}
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
                        { label: "Restaurar", icon: <History />, disabled: !active || backup.status !== "AVAILABLE", onSelect: () => setRestoring(backup) },
                        {
                          label: "Baixar",
                          icon: <Download />,
                          disabled: backup.status !== "AVAILABLE",
                          onSelect: () => window.open(backupFileUrl(server.id, backup.id), "_self"),
                        },
                        {
                          label: "Apagar",
                          icon: <Trash2 />,
                          destructive: true,
                          disabled: backup.status === "CREATING",
                          onSelect: () => setDiscarding(backup),
                        },
                      ]}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Card>

      <ConfirmDialog
        open={restoring !== null}
        onOpenChange={(open) => !open && setRestoring(null)}
        title={restoring ? `Voltar ao backup "${restoring.name}"?` : "Restaurar"}
        description="O servidor volta exatamente ao estado do backup. Tudo o que mudou depois disso se perde."
        confirmLabel="Restaurar"
        destructive
        loading={isRestoring}
        onConfirm={() => restoring && restore({ id: server.id, backupId: restoring.id })}
      />
      <ConfirmDialog
        open={discarding !== null}
        onOpenChange={(open) => !open && setDiscarding(null)}
        title={discarding ? `Apagar "${discarding.name}"?` : "Apagar"}
        description="O backup é apagado e o lugar volta a ficar livre."
        confirmLabel="Apagar"
        destructive
        loading={isDiscarding}
        onConfirm={() => discarding && discard({ id: server.id, backupId: discarding.id })}
      />
    </div>
  );
};
