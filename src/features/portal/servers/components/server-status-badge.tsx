import { Badge } from "@/components";
import type { ServerDto } from "../api";

type Tone = "success" | "neutral" | "warning" | "info" | "destructive";

/** Uma só situação para o cliente, juntando a assinatura e a máquina. */
export const serverState = (server: ServerDto): { label: string; tone: Tone; running: boolean } => {
  switch (server.subscriptionStatus) {
    case "PENDING_PAYMENT":
      return { label: "Aguardando pagamento", tone: "warning", running: false };
    case "PROVISIONING":
      return { label: "Sendo criado", tone: "info", running: false };
    case "SUSPENDED":
      return { label: "Suspenso", tone: "destructive", running: false };
    case "CANCELED":
      return { label: "Cancelado", tone: "neutral", running: false };
    default:
      break;
  }
  switch (server.machineStatus) {
    case "RUNNING":
      return { label: "Ligado", tone: "success", running: true };
    case "STOPPED":
      return { label: "Desligado", tone: "neutral", running: false };
    case "CREATING":
      return { label: "Em manutenção", tone: "info", running: false };
    default:
      return { label: "Com problema", tone: "destructive", running: false };
  }
};

export const ServerStatusBadge = ({ server }: { server: ServerDto }) => {
  const state = serverState(server);
  return <Badge tone={state.tone}>{state.label}</Badge>;
};
