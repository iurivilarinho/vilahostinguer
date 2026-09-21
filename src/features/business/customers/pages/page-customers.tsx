import { KeyRound, Lock, LockOpen, MoreHorizontal, Search, UserX, Users } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DropdownMenu,
  EmptyState,
  FieldWrapper,
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
  Textarea,
  Typography,
} from "@/components";
import { usePaginatedData } from "@/app/hooks/use-paginated-data";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { Rotas } from "@/app/variables/rotas";
import { formatDateTime, formatRelative } from "@/lib/format";
import {
  useCustomersQuery,
  useCustomerStatusMutation,
  useResetCustomerPasswordMutation,
  type CustomerDto,
  type CustomerFilter,
  type CustomerStatus,
} from "../api";

const STATUS_TONE: Record<CustomerStatus, "success" | "warning" | "neutral"> = { ACTIVE: "success", SUSPENDED: "warning", CLOSED: "neutral" };
const SORT = [{ by: "createdAt" as const, direction: "desc" as const }];

type StatusChange = { customer: CustomerDto; status: CustomerStatus };

const STATUS_COPY: Record<CustomerStatus, { title: string; description: string }> = {
  ACTIVE: { title: "Desbloquear", description: "O cliente volta a contratar e a mexer nos servidores." },
  SUSPENDED: { title: "Bloquear", description: "O cliente só entra para ver e pagar faturas. Os servidores continuam ligados." },
  CLOSED: { title: "Encerrar conta", description: "A conta deixa de entrar no painel. Só é possível sem assinaturas ativas." },
};

export const PageCustomers = () => {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [change, setChange] = useState<StatusChange | null>(null);
  const [reason, setReason] = useState("");
  const [resetting, setResetting] = useState<CustomerDto | null>(null);
  const [password, setPassword] = useState("");
  const filter: CustomerFilter = { search: search.trim() || undefined };
  const { data: customers, isLoading, isError, error, refetch, isFetching, pagination, updatePagination } = usePaginatedData<CustomerDto, CustomerFilter>({
    query: useCustomersQuery,
    filter,
    sort: SORT,
    storageKey: "customersPagination",
  });
  const { mutate: changeStatus, isPending: isChanging } = useCustomerStatusMutation({ onSuccess: () => setChange(null) });
  const { mutate: resetPassword, isPending: isResetting } = useResetCustomerPasswordMutation({
    onSuccess: () => {
      setResetting(null);
      setPassword("");
    },
  });

  return (
    <>
      <PageHeader title="Clientes" description="Quem se cadastrou no painel do cliente." />
      <div className="relative w-80 max-w-full">
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Nome, e-mail ou documento" className="pl-9" />
      </div>
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os clientes.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : (
        <Card>
          {isLoading ? (
            <div className="p-5">
              <Skeleton className="h-24 w-full" />
            </div>
          ) : customers.length === 0 ? (
            <EmptyState icon={<Users />} title="Nenhum cliente" description="Os clientes aparecem aqui quando se cadastram no painel do cliente." />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Cliente</TableHead>
                    <TableHead>Contato</TableHead>
                    <TableHead>Cadastro</TableHead>
                    <TableHead>Último acesso</TableHead>
                    <TableHead>Situação</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {customers.map((customer) => (
                    <TableRow key={customer.id}>
                      <TableCell>
                        <Typography variant="body-sm" className="font-medium">
                          {customer.name}
                        </Typography>
                        <Typography variant="caption">{customer.email}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{customer.phone ?? "—"}</Typography>
                        <Typography variant="caption">{customer.document ?? ""}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{formatDateTime(customer.createdAt)}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body-sm">{formatRelative(customer.lastLoginAt)}</Typography>
                      </TableCell>
                      <TableCell>
                        <Badge tone={STATUS_TONE[customer.status]}>{customer.statusDescription}</Badge>
                      </TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={
                            <Button variant="ghost" size="icon" aria-label={`Ações de ${customer.name}`}>
                              <MoreHorizontal />
                            </Button>
                          }
                          items={[
                            {
                              label: "Ver assinaturas",
                              icon: <Users />,
                              onSelect: () => navigate(`${Rotas.business.subscriptions}?cliente=${customer.id}`),
                            },
                            { label: "Definir senha nova", icon: <KeyRound />, disabled: customer.status === "CLOSED", onSelect: () => setResetting(customer) },
                            customer.status === "SUSPENDED"
                              ? { label: "Desbloquear", icon: <LockOpen />, onSelect: () => setChange({ customer, status: "ACTIVE" }) }
                              : { label: "Bloquear", icon: <Lock />, disabled: customer.status === "CLOSED", onSelect: () => setChange({ customer, status: "SUSPENDED" }) },
                            {
                              label: "Encerrar conta",
                              icon: <UserX />,
                              destructive: true,
                              disabled: customer.status === "CLOSED",
                              onSelect: () => setChange({ customer, status: "CLOSED" }),
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
        </Card>
      )}

      <ConfirmDialog
        open={change !== null}
        onOpenChange={(open) => !open && setChange(null)}
        title={change ? `${STATUS_COPY[change.status].title}: ${change.customer.name}?` : ""}
        description={change ? STATUS_COPY[change.status].description : ""}
        confirmLabel={change ? STATUS_COPY[change.status].title : "Confirmar"}
        destructive={change?.status !== "ACTIVE"}
        loading={isChanging}
        onConfirm={() => change && changeStatus({ id: change.customer.id, status: change.status, reason })}
      >
        <FieldWrapper label="Motivo (fica na auditoria)" htmlFor="customer-reason">
          <Textarea id="customer-reason" rows={2} value={reason} onChange={(event) => setReason(event.target.value)} />
        </FieldWrapper>
      </ConfirmDialog>
      <ConfirmDialog
        open={resetting !== null}
        onOpenChange={(open) => !open && setResetting(null)}
        title={resetting ? `Senha nova para ${resetting.name}` : ""}
        description="O cliente entra com esta senha e as sessões abertas dele são encerradas. Passe a senha por um canal seguro."
        confirmLabel="Definir senha"
        loading={isResetting}
        onConfirm={() => resetting && password.length >= 8 && resetPassword({ id: resetting.id, password })}
      >
        <FieldWrapper label="Nova senha (mínimo 8)" htmlFor="customer-password">
          <Input id="customer-password" type="text" autoComplete="off" value={password} onChange={(event) => setPassword(event.target.value)} />
        </FieldWrapper>
      </ConfirmDialog>
    </>
  );
};
