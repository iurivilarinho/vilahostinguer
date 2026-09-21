import { SquareTerminal } from "lucide-react";
import { Card, EmptyState } from "@/components";
import { TerminalView } from "@/features/terminal";
import type { ServerDto } from "../api";
import { serverState } from "../components/server-status-badge";

/** Terminal no navegador, como root dentro do servidor. */
export const SectionTerminal = ({ server }: { server: ServerDto }) => {
  if (server.subscriptionStatus !== "ACTIVE" || !serverState(server).running) {
    return (
      <Card>
        <EmptyState icon={<SquareTerminal />} title="Servidor desligado" description="Ligue o servidor para abrir o terminal." />
      </Card>
    );
  }
  return <TerminalView serverId={server.id} className="h-[65vh]" />;
};
