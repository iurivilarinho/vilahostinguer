import { Activity, Archive, Boxes, Globe, KeyRound, LayoutDashboard, Palette, Server, Settings, SquareTerminal } from "lucide-react";
import type { ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { Typography } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { cn } from "@/lib/merge-classes";
import { ScanStatusCard } from "./scan-status-card";

type NavItem = {
  to: string;
  label: string;
  icon: ReactNode;
  end?: boolean;
};

const MAIN_ITEMS: NavItem[] = [
  { to: Rotas.dashboard, label: "Início", icon: <LayoutDashboard />, end: true },
  { to: Rotas.devices.list, label: "Dispositivos", icon: <Server /> },
  { to: Rotas.machines, label: "Máquinas", icon: <Boxes /> },
  { to: Rotas.remoteAccess, label: "Acesso remoto", icon: <Globe /> },
  { to: Rotas.terminal, label: "Terminal", icon: <SquareTerminal /> },
  { to: Rotas.backups, label: "Backups", icon: <Archive /> },
  { to: Rotas.operations, label: "Atividades", icon: <Activity /> },
];

const ACCOUNT_ITEMS: NavItem[] = [
  { to: Rotas.credentials, label: "Credenciais", icon: <KeyRound /> },
  { to: Rotas.settings, label: "Configurações", icon: <Settings /> },
  { to: Rotas.designSystem, label: "Design system", icon: <Palette /> },
];

type NavGroupProps = {
  title: string;
  items: NavItem[];
};

const NavGroup = ({ title, items }: NavGroupProps) => (
  <div className="flex flex-col gap-1">
    <Typography variant="section-label" className="px-3 pb-1">
      {title}
    </Typography>
    {items.map((item) => (
      <NavLink
        key={item.to}
        to={item.to}
        end={item.end}
        className={({ isActive }) =>
          cn(
            "nav-link flex items-center gap-3 rounded-lg px-3 py-2.5 text-sidebar-foreground transition-colors hover:bg-muted [&_svg]:size-5",
            isActive && "bg-primary-soft text-primary-soft-foreground hover:bg-primary-soft",
          )
        }
      >
        {item.icon}
        {item.label}
      </NavLink>
    ))}
  </div>
);

export const Sidebar = () => (
  <aside className="flex h-full w-64 shrink-0 flex-col border-r border-sidebar-border bg-sidebar">
    <div className="flex items-center gap-3 px-5 py-5">
      <img src="/icon.svg" alt="" className="size-9" />
      <div className="flex flex-col">
        <Typography variant="title-md" as="span">
          Bancada
        </Typography>
        <Typography variant="caption">Servidores em casa</Typography>
      </div>
    </div>
    <nav className="flex flex-1 flex-col gap-6 overflow-y-auto px-3 py-2">
      <NavGroup title="Painel" items={MAIN_ITEMS} />
      <NavGroup title="Conta" items={ACCOUNT_ITEMS} />
    </nav>
    <div className="p-3">
      <ScanStatusCard />
    </div>
  </aside>
);
