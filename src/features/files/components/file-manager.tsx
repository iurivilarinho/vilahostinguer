import { ArrowUp, Download, File, Folder, FolderPlus, Link2, RefreshCw, Trash2 } from "lucide-react";
import { useState, type FormEvent } from "react";
import {
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  FileUI,
  Input,
  QueryErrorState,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tooltip,
  Typography,
} from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { formatBytes, formatDateTime } from "@/lib/format";
import { fileDownloadUrl, useDeleteEntryMutation, useDirectoryQuery, useUploadFileMutation, type FileEntryDto } from "../api";
import { NewFolderDialog } from "./new-folder-dialog";

const parentOf = (path: string): string => {
  if (path === "/") {
    return "/";
  }
  const parent = path.substring(0, path.lastIndexOf("/"));
  return parent === "" ? "/" : parent;
};

type FileManagerProps = {
  deviceId: number;
  initialPath?: string;
};

export const FileManager = ({ deviceId, initialPath = "/" }: FileManagerProps) => {
  const [path, setPath] = useState(initialPath);
  const [pathInput, setPathInput] = useState(initialPath);
  const [folderOpen, setFolderOpen] = useState(false);
  const [deleting, setDeleting] = useState<FileEntryDto | null>(null);
  const { data: entries, isLoading, isError, error, refetch, isFetching } = useDirectoryQuery(deviceId, path);
  const { mutate: upload, isPending: isUploading } = useUploadFileMutation();
  const { mutate: deleteEntry, isPending: isDeleting } = useDeleteEntryMutation();

  const navigate = (target: string) => {
    setPath(target);
    setPathInput(target);
  };

  const handlePathSubmit = (event: FormEvent) => {
    event.preventDefault();
    const target = pathInput.trim() || "/";
    navigate(target.length > 1 && target.endsWith("/") ? target.slice(0, -1) : target);
  };

  const segments = path.split("/").filter(Boolean);

  return (
    <Card>
      <div className="flex flex-wrap items-center gap-2 border-b border-border px-4 py-3">
        <Tooltip content="Pasta acima">
          <Button variant="outline" size="icon" onClick={() => navigate(parentOf(path))} disabled={path === "/"} aria-label="Pasta acima">
            <ArrowUp />
          </Button>
        </Tooltip>
        <form onSubmit={handlePathSubmit} className="min-w-64 flex-1">
          <Input value={pathInput} onChange={(event) => setPathInput(event.target.value)} className="font-mono" aria-label="Caminho" />
        </form>
        <Button variant="outline" size="icon" onClick={() => refetch()} aria-label="Recarregar" disabled={isFetching}>
          <RefreshCw className={isFetching ? "animate-spin" : undefined} />
        </Button>
        <Button variant="outline" onClick={() => setFolderOpen(true)}>
          <FolderPlus />
          Nova pasta
        </Button>
        <FileUI.Input
          label={isUploading ? "Enviando..." : "Enviar"}
          multiple
          disabled={isUploading}
          onFilesSelected={(files) => files.forEach((file) => upload({ deviceId, path, file }))}
        />
      </div>

      <nav aria-label="Caminho" className="flex flex-wrap items-center gap-1 px-4 pt-3">
        <button type="button" className="inline-link cursor-pointer text-sm" onClick={() => navigate("/")}>
          /
        </button>
        {segments.map((segment, index) => {
          const target = `/${segments.slice(0, index + 1).join("/")}`;
          return (
            <span key={target} className="flex items-center gap-1">
              <button type="button" className="inline-link cursor-pointer text-sm" onClick={() => navigate(target)}>
                {segment}
              </button>
              {index < segments.length - 1 && <Typography variant="caption">/</Typography>}
            </span>
          );
        })}
      </nav>

      <div className="p-2">
        {isError ? (
          <QueryErrorState message={getApiErrorMessage(error, "Não foi possível abrir a pasta.")} onRetry={() => refetch()} retrying={isFetching} className="m-2" />
        ) : isLoading ? (
          <div className="flex flex-col gap-2 p-3">
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
          </div>
        ) : !entries || entries.length === 0 ? (
          <EmptyState icon={<Folder />} title="Pasta vazia" description="Envie arquivos ou crie uma pasta aqui." />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nome</TableHead>
                <TableHead>Tamanho</TableHead>
                <TableHead>Permissões</TableHead>
                <TableHead>Modificado</TableHead>
                <TableHead className="w-24" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {entries.map((entry) => (
                <TableRow key={entry.path}>
                  <TableCell>
                    {entry.directory ? (
                      <button type="button" className="flex cursor-pointer items-center gap-2 text-left hover:text-primary" onClick={() => navigate(entry.path)}>
                        <Folder className="size-4 shrink-0 text-primary" />
                        <Typography variant="body-sm" as="span" className="font-medium">
                          {entry.name}
                        </Typography>
                      </button>
                    ) : (
                      <span className="flex items-center gap-2">
                        {entry.symlink ? <Link2 className="size-4 shrink-0 text-muted-foreground" /> : <File className="size-4 shrink-0 text-muted-foreground" />}
                        <Typography variant="body-sm" as="span">
                          {entry.name}
                        </Typography>
                      </span>
                    )}
                  </TableCell>
                  <TableCell>
                    <Typography variant="body-sm">{entry.directory ? "—" : formatBytes(entry.sizeBytes)}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="mono" className="text-muted-foreground">
                      {entry.permissions}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body-sm">{formatDateTime(entry.modifiedAt)}</Typography>
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      {!entry.directory && (
                        <Tooltip content="Baixar">
                          <a href={fileDownloadUrl(deviceId, entry.path)} download={entry.name} className="inline-flex size-9 items-center justify-center rounded-lg hover:bg-muted" aria-label={`Baixar ${entry.name}`}>
                            <Download className="size-4" />
                          </a>
                        </Tooltip>
                      )}
                      <Tooltip content="Apagar">
                        <Button variant="ghost" size="icon" onClick={() => setDeleting(entry)} aria-label={`Apagar ${entry.name}`}>
                          <Trash2 className="text-destructive-foreground" />
                        </Button>
                      </Tooltip>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <NewFolderDialog open={folderOpen} onOpenChange={setFolderOpen} deviceId={deviceId} parentPath={path} />
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={deleting ? `Apagar ${deleting.name}?` : "Apagar"}
        description={deleting?.directory ? "Só pastas vazias podem ser apagadas por aqui." : "O arquivo é apagado do dispositivo. Não há lixeira."}
        confirmLabel="Apagar"
        destructive
        loading={isDeleting}
        onConfirm={() => {
          if (deleting) {
            deleteEntry({ deviceId, path: deleting.path }, { onSettled: () => setDeleting(null) });
          }
        }}
      />
    </Card>
  );
};
