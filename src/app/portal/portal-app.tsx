import { AppProvider } from "@/app/providers/app-provider";
import { PortalRouter } from "./routing/portal-router";

/** Painel do cliente: mesma base visual e de dados do administrativo, outra aplicação. */
export const PortalApp = () => (
  <AppProvider>
    <PortalRouter />
  </AppProvider>
);
