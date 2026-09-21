import { Globe, LayoutGrid, Route as RouteIcon } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { PageHeader, QueryErrorState, Skeleton, Tabs } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { useGatewayStatusQuery, useRefreshUpnpMutation } from "../api";
import { DomainsPanel } from "../components/domains-panel";
import { GatewayStatusPanel } from "../components/gateway-status-panel";
import { RoutesPanel } from "../components/routes-panel";
import { GatewaySettingsCard } from "../form/gateway-settings-card";

type RemoteAccessTab = "visao-geral" | "dominios" | "rotas";

const TABS = [
  { value: "visao-geral" as const, label: "Visão geral", icon: <LayoutGrid /> },
  { value: "rotas" as const, label: "Rotas", icon: <RouteIcon /> },
  { value: "dominios" as const, label: "Domínios e DDNS", icon: <Globe /> },
];

const isTab = (value: string | null): value is RemoteAccessTab => TABS.some((tab) => tab.value === value);

export const PageRemoteAccess = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get("aba");
  const tab: RemoteAccessTab = isTab(requested) ? requested : "visao-geral";
  const { data: gateway, isLoading, isError, error, refetch, isFetching } = useGatewayStatusQuery();
  const { mutate: refreshUpnp, isPending: isRefreshingUpnp } = useRefreshUpnpMutation();

  return (
    <>
      <PageHeader
        title="Acesso remoto"
        description="Domínios com DNS dinâmico e rotas que levam quem está fora de casa até os seus dispositivos e máquinas."
      />
      <Tabs value={tab} onValueChange={(value) => setSearchParams({ aba: value })} items={TABS} />
      {tab === "visao-geral" &&
        (isError ? (
          <QueryErrorState message={getApiErrorMessage(error, "Não foi possível ler a situação do acesso remoto.")} onRetry={() => refetch()} retrying={isFetching} />
        ) : isLoading || !gateway ? (
          <Skeleton className="h-40 w-full" />
        ) : (
          <div className="flex flex-col gap-4">
            <GatewayStatusPanel status={gateway} onRefreshUpnp={() => refreshUpnp()} refreshingUpnp={isRefreshingUpnp} />
            <GatewaySettingsCard status={gateway} />
          </div>
        ))}
      {tab === "rotas" && <RoutesPanel gateway={gateway} />}
      {tab === "dominios" && <DomainsPanel publicIp={gateway?.publicIp ?? null} />}
    </>
  );
};
