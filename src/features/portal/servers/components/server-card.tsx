import { Server } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, Typography, buttonVariants } from "@/components";
import { RotasPortal } from "@/app/variables/rotas-portal";
import { cn } from "@/lib/merge-classes";
import type { ServerDto } from "../api";
import { ServerStatusBadge, serverState } from "./server-status-badge";

/** Servidor na lista: nome, plano, sistema, situação e o atalho para gerenciar. */
export const ServerCard = ({ server }: { server: ServerDto }) => {
  const state = serverState(server);
  return (
    <Card className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
      <div className={cn("flex size-12 shrink-0 items-center justify-center rounded-lg", state.running ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground")}>
        <Server className="size-6" />
      </div>
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <Typography variant="title-sm" className="truncate">
            {server.hostname}
          </Typography>
          <ServerStatusBadge server={server} />
        </div>
        <Typography variant="caption">
          {server.planName} · {server.distributionName} {server.version} · {server.cpuLimit} CPU · {server.memoryMb} MB
        </Typography>
        {server.sshCommand && (
          <Typography variant="mono" className="truncate text-muted-foreground">
            {server.sshCommand}
          </Typography>
        )}
        {server.notice && server.subscriptionStatus !== "ACTIVE" && <Typography variant="caption">{server.notice}</Typography>}
      </div>
      <Link to={RotasPortal.server(server.id)} className={buttonVariants({ variant: "secondary" })}>
        Gerenciar
      </Link>
    </Card>
  );
};
