import { Navigate, Outlet, useLocation } from "react-router-dom";
import { Skeleton } from "@/components";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { useSessionQuery } from "@/features/portal/session/api";

/** Área do cliente: sem sessão, vai para a entrada e volta para onde estava depois. */
export const RequireSession = () => {
  const location = useLocation();
  const { data: customer, isLoading } = useSessionQuery();

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center p-10">
        <Skeleton className="h-40 w-full max-w-3xl" />
      </div>
    );
  }
  if (!customer) {
    return <Navigate to={`${RotasPortal.login}?voltar=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  }
  return <Outlet />;
};

/** Entrada e cadastro: quem já está logado vai direto para o painel. */
export const RedirectIfSession = () => {
  const { data: customer, isLoading } = useSessionQuery();
  if (isLoading) {
    return null;
  }
  return customer ? <Navigate to={RotasPortal.area} replace /> : <Outlet />;
};
