import { Moon, Sun } from "lucide-react";
import { Outlet } from "react-router-dom";
import { Button, Tooltip } from "@/components";
import { DeviceEventsWatcher } from "@/app/events/device-events-watcher";
import { useThemeStore } from "@/app/theme/use-theme-store";
import { OperationLogDialog } from "@/features/operations";
import { Sidebar } from "./sidebar";

export const AppLayout = () => {
  const theme = useThemeStore((state) => state.theme);
  const toggleTheme = useThemeStore((state) => state.toggle);

  return (
    <div className="flex h-full overflow-hidden">
      <DeviceEventsWatcher />
      <OperationLogDialog />
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 shrink-0 items-center justify-end gap-2 border-b border-border bg-card px-6">
          <Tooltip content={theme === "dark" ? "Tema claro" : "Tema escuro"}>
            <Button variant="ghost" size="icon" onClick={toggleTheme} aria-label="Alternar tema">
              {theme === "dark" ? <Sun /> : <Moon />}
            </Button>
          </Tooltip>
        </header>
        <main className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
