import { CreditCard, Home, LogOut, Moon, Plus, Server, Sun, UserRound } from "lucide-react";
import type { ReactNode } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { Button, DropdownMenu, Tooltip, Typography, buttonVariants } from "@/components";
import { useThemeStore } from "@/app/theme/use-theme-store";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { useLogoutMutation, useSessionQuery } from "@/features/portal/session/api";
import { cn } from "@/lib/merge-classes";
import { PortalBrand } from "./portal-brand";

type NavItem = {
  to: string;
  label: string;
  icon: ReactNode;
  end?: boolean;
};

const NAV_ITEMS: NavItem[] = [
  { to: RotasPortal.area, label: "Início", icon: <Home />, end: true },
  { to: RotasPortal.servers, label: "VPS", icon: <Server /> },
  { to: RotasPortal.billing, label: "Faturamento", icon: <CreditCard /> },
  { to: RotasPortal.account, label: "Conta", icon: <UserRound /> },
];

const navClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    "nav-link flex items-center gap-3 rounded-lg px-3 py-2.5 text-sidebar-foreground transition-colors hover:bg-muted [&_svg]:size-5",
    isActive && "bg-primary-soft text-primary-soft-foreground hover:bg-primary-soft",
  );

/** Área do cliente no estilo do hPanel: menu lateral, barra superior com a conta e o conteúdo. */
export const AreaLayout = () => {
  const navigate = useNavigate();
  const { data: customer } = useSessionQuery();
  const { mutate: logout } = useLogoutMutation({ onSuccess: () => navigate(RotasPortal.home) });
  const theme = useThemeStore((state) => state.theme);
  const toggleTheme = useThemeStore((state) => state.toggle);
  const firstName = customer?.name.split(" ")[0] ?? "";

  return (
    <div className="flex h-full overflow-hidden">
      <aside className="hidden w-64 shrink-0 flex-col border-r border-sidebar-border bg-sidebar md:flex">
        <div className="px-5 py-5">
          <PortalBrand to={RotasPortal.area} />
        </div>
        <nav className="flex flex-1 flex-col gap-1 px-3 py-2">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={navClass}>
              {item.icon}
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="p-3">
          <Link to={`${RotasPortal.home}#planos`} className={cn(buttonVariants({ variant: "primary" }), "w-full")}>
            <Plus />
            Contratar servidor
          </Link>
        </div>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 shrink-0 items-center justify-between gap-2 border-b border-border bg-card px-4 md:justify-end md:px-6">
          <PortalBrand to={RotasPortal.area} className="md:hidden" />
          <div className="flex items-center gap-1">
            <Tooltip content={theme === "dark" ? "Tema claro" : "Tema escuro"}>
              <Button variant="ghost" size="icon" onClick={toggleTheme} aria-label="Alternar tema">
                {theme === "dark" ? <Sun /> : <Moon />}
              </Button>
            </Tooltip>
            <DropdownMenu
              trigger={
                <Button variant="ghost" className="gap-2">
                  <span className="flex size-8 items-center justify-center rounded-full bg-primary-soft text-sm font-semibold text-primary-soft-foreground">
                    {firstName.charAt(0).toUpperCase()}
                  </span>
                  <Typography variant="body-sm" as="span" className="hidden font-medium sm:inline">
                    {firstName}
                  </Typography>
                </Button>
              }
              items={[
                { label: "Minha conta", icon: <UserRound />, onSelect: () => navigate(RotasPortal.account) },
                { label: "Sair", icon: <LogOut />, onSelect: () => logout() },
              ]}
            />
          </div>
        </header>
        <nav className="flex gap-1 overflow-x-auto border-b border-border bg-card px-2 py-1 md:hidden">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => cn(navClass({ isActive }), "shrink-0 py-2")}>
              {item.icon}
              {item.label}
            </NavLink>
          ))}
        </nav>
        <main className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 p-4 md:p-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
