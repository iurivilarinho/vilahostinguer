import { Archive, ArchiveRestore, Eye, KeyRound, MoreHorizontal, Pencil, Plus, Search, Star } from "lucide-react";
import { useState } from "react";
import {
  Badge,
  Button,
  Card,
  DropdownMenu,
  EmptyState,
  Input,
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
import { formatDateTime } from "@/lib/format";
import { useChangeCredentialActiveMutation, useCredentialsQuery, type CredentialDto, type CredentialFilter } from "../api";
import { RevealSecretDialog } from "../components/reveal-secret-dialog";
import { CredentialSheet } from "../form/credential-sheet";

type ListView = "active" | "archived";

const SORT_BY_NAME = [{ by: "name" as const, direction: "asc" as const }];

export const PageCredentials = () => {
  const [view, setView] = useState<ListView>("active");
  const [search, setSearch] = useState("");
  const [sheetOpen, setSheetOpen] = useState(false);
  const [editing, setEditing] = useState<CredentialDto | undefined>();
  const [revealing, setRevealing] = useState<CredentialDto | null>(null);
  const { mutate: changeActive } = useChangeCredentialActiveMutation();

  const filter: CredentialFilter = { search: search.trim() || undefined, active: view === "active" };
  const {
    data: credentials,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
    pagination,
    updatePagination,
  } = usePaginatedData<CredentialDto, CredentialFilter>({
    query: useCredentialsQuery,
    filter,
    sort: SORT_BY_NAME,
    storageKey: "credentialsPagination",
  });

  const openCreate = () => {
    setEditing(undefined);
    setSheetOpen(true);
  };

  const openEdit = (credential: CredentialDto) => {
    setEditing(credential);
    setSheetOpen(true);
  };

  return (
    <>
      <PageHeader
        title="Credenciais"
        description="Usuários e senhas ou chaves SSH, cifrados neste computador."
        actions={
          <Button onClick={openCreate}>
            <Plus />
            Nova credencial
          </Button>
        }
      />

      <div className="flex flex-wrap items-end justify-between gap-4">
        <Tabs
          value={view}
          onValueChange={setView}
          items={[
            { value: "active", label: "Ativas" },
            { value: "archived", label: "Arquivadas" },
          ]}
        />
        <div className="relative w-72">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por nome ou usuário" className="pl-9" />
        </div>
      </div>

      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar as credenciais.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : (
        <Card>
          {isLoading ? (
            <div className="flex flex-col gap-3 p-5">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
            </div>
          ) : credentials.length === 0 ? (
            <EmptyState
              icon={<KeyRound />}
              title={view === "active" ? "Nenhuma credencial ainda" : "Nenhuma credencial arquivada"}
              description="Cadastre o usuário e a senha (ou a chave) dos seus dispositivos. Marque uma como padrão para que aparelhos novos sejam configurados sozinhos."
              action={
                view === "active" && (
                  <Button onClick={openCreate}>
                    <Plus />
                    Nova credencial
                  </Button>
                )
              }
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nome</TableHead>
                    <TableHead>Usuário</TableHead>
                    <TableHead>Autenticação</TableHead>
                    <TableHead>Atualizada</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {credentials.map((credential) => (
                    <TableRow key={credential.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Typography variant="body-sm" className="font-medium">
                            {credential.name}
                          </Typography>
                          {credential.defaultCredential && (
                            <Badge tone="primary">
                              <Star className="size-3" />
                              Padrão
                            </Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Typography variant="mono">{credential.username}</Typography>
                      </TableCell>
                      <TableCell>
                        <Badge>{credential.authTypeDescription}</Badge>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{formatDateTime(credential.updatedAt)}</Typography>
                      </TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={
                            <Button variant="ghost" size="icon" aria-label={`Ações de ${credential.name}`}>
                              <MoreHorizontal />
                            </Button>
                          }
                          items={[
                            { label: "Editar", icon: <Pencil />, onSelect: () => openEdit(credential) },
                            { label: "Revelar segredo", icon: <Eye />, onSelect: () => setRevealing(credential) },
                            credential.active
                              ? {
                                  label: "Arquivar",
                                  icon: <Archive />,
                                  destructive: true,
                                  onSelect: () => changeActive({ id: credential.id, active: false }),
                                }
                              : { label: "Reativar", icon: <ArchiveRestore />, onSelect: () => changeActive({ id: credential.id, active: true }) },
                          ]}
                        />
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
      )}

      <CredentialSheet open={sheetOpen} onOpenChange={setSheetOpen} credential={editing} />
      <RevealSecretDialog credential={revealing} onClose={() => setRevealing(null)} />
    </>
  );
};
