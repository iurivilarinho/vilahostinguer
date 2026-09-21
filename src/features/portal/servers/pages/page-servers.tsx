import { Plus, Server } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, EmptyState, PageHeader, QueryErrorState, Skeleton, buttonVariants } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { useServersQuery } from "../api";
import { ServerCard } from "../components/server-card";

export const PageServers = () => {
  const { data: servers, isLoading, isError, error, refetch, isFetching } = useServersQuery();
  const newServer = (
    <Link to={`${RotasPortal.home}#planos`} className={buttonVariants()}>
      <Plus />
      Contratar servidor
    </Link>
  );

  return (
    <>
      <PageHeader title="VPS" description="Seus servidores virtuais privados." actions={newServer} />
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os servidores.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : isLoading ? (
        <Skeleton className="h-28 w-full" />
      ) : (servers?.length ?? 0) === 0 ? (
        <Card>
          <EmptyState icon={<Server />} title="Nenhum servidor ainda" description="Escolha um plano, pague com Pix e o servidor fica pronto em minutos." action={newServer} />
        </Card>
      ) : (
        <div className="flex flex-col gap-3">
          {servers?.map((server) => (
            <ServerCard key={server.id} server={server} />
          ))}
        </div>
      )}
    </>
  );
};
