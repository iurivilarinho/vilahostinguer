import { Link, Outlet } from "react-router-dom";
import { Typography, buttonVariants } from "@/components";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { usePortalInfoQuery } from "@/features/portal/catalog/api";
import { useSessionQuery } from "@/features/portal/session/api";
import { PortalBrand } from "./portal-brand";

/** Vitrine e telas de acesso: barra superior com a marca e rodapé com o suporte. */
export const PublicLayout = () => {
  const { data: customer } = useSessionQuery();
  const { data: info } = usePortalInfoQuery();
  const year = new Date().getFullYear();

  return (
    <div className="flex min-h-full flex-col bg-background">
      <header className="sticky top-0 z-30 border-b border-border bg-card/95 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between gap-4 px-4">
          <PortalBrand />
          <nav className="flex items-center gap-2">
            <a href={`${RotasPortal.home}#planos`} className="hidden px-3 text-sm font-medium text-foreground hover:text-primary sm:inline">
              Planos
            </a>
            {customer ? (
              <Link to={RotasPortal.area} className={buttonVariants({ variant: "primary" })}>
                Meu painel
              </Link>
            ) : (
              <>
                <Link to={RotasPortal.login} className={buttonVariants({ variant: "ghost" })}>
                  Entrar
                </Link>
                {info?.registrationOpen !== false && (
                  <Link to={RotasPortal.register} className={buttonVariants({ variant: "primary" })}>
                    Criar conta
                  </Link>
                )}
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t border-border bg-card">
        <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-6">
          <Typography variant="caption">
            © {year} {info?.companyName ?? ""}. Servidores Linux com acesso root.
          </Typography>
          {info?.supportEmail && (
            <a href={`mailto:${info.supportEmail}`} className="inline-link text-sm">
              {info.supportEmail}
            </a>
          )}
        </div>
      </footer>
    </div>
  );
};
