import { Globe, Plus } from "lucide-react";
import { useState } from "react";
import { Button, Card, EmptyState, QueryErrorState, Skeleton, Tabs } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { useChangeDomainActiveMutation, useDomainsQuery, useSyncDomainMutation, type DomainDto } from "../api";
import { DomainSheet } from "../form/domain-sheet";
import { DomainCard } from "./domain-card";

type DomainsPanelProps = {
  publicIp: string | null;
};

type DomainView = "active" | "archived";

/** Domínios cadastrados, com o resultado do DDNS de cada um. */
export const DomainsPanel = ({ publicIp }: DomainsPanelProps) => {
  const [view, setView] = useState<DomainView>("active");
  const [sheetOpen, setSheetOpen] = useState(false);
  const [editing, setEditing] = useState<DomainDto | undefined>();
  const { data, isLoading, isError, error, refetch, isFetching } = useDomainsQuery({
    page: 0,
    size: 100,
    sort: [{ by: "name", direction: "asc" }],
    filter: { active: view === "active" },
  });
  const { mutate: sync, isPending: isSyncing, variables: syncingId } = useSyncDomainMutation();
  const { mutate: changeActive } = useChangeDomainActiveMutation();
  const domains = data?.data ?? [];

  const openCreate = () => {
    setEditing(undefined);
    setSheetOpen(true);
  };

  const newDomain = (
    <Button onClick={openCreate}>
      <Plus />
      Novo domínio
    </Button>
  );

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <Tabs
          value={view}
          onValueChange={setView}
          items={[
            { value: "active", label: "Ativos" },
            { value: "archived", label: "Arquivados" },
          ]}
        />
        {newDomain}
      </div>
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os domínios.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-52" />
          <Skeleton className="h-52" />
        </div>
      ) : domains.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Globe />}
            title={view === "active" ? "Nenhum domínio ainda" : "Nenhum domínio arquivado"}
            description="Um domínio dá um nome fixo à sua rede, mesmo quando a operadora troca o IP. O DuckDNS é grátis e leva um minuto para criar."
            action={view === "active" && newDomain}
          />
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {domains.map((domain) => (
            <DomainCard
              key={domain.id}
              domain={domain}
              publicIp={publicIp}
              syncing={isSyncing && syncingId === domain.id}
              onSync={(target) => sync(target.id)}
              onEdit={(target) => {
                setEditing(target);
                setSheetOpen(true);
              }}
              onChangeActive={(target, active) => changeActive({ id: target.id, active })}
            />
          ))}
        </div>
      )}
      <DomainSheet open={sheetOpen} onOpenChange={setSheetOpen} domain={editing} />
    </div>
  );
};
