import { ExternalLink } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button, Typography } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { TerminalView, useTerminalSessionsStore } from "@/features/terminal";
import { RequiresAccess } from "../../components/requires-access";
import { useDeviceOutlet } from "../device-outlet";

export const SectionTerminal = () => {
  const { device } = useDeviceOutlet();
  const navigate = useNavigate();
  const openSession = useTerminalSessionsStore((state) => state.openSession);

  const openInTerminalPage = () => {
    openSession(device.id, device.name);
    navigate(Rotas.terminal);
  };

  return (
    <RequiresAccess device={device}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Typography variant="body-sm" className="text-muted-foreground">
          Shell de {device.credential?.username ?? "usuário"} em {device.name}. Fechar esta tela encerra a sessão.
        </Typography>
        <Button variant="outline" size="sm" onClick={openInTerminalPage}>
          <ExternalLink />
          Abrir na tela de Terminal
        </Button>
      </div>
      <TerminalView deviceId={device.id} className="h-[calc(100vh-24rem)] min-h-96" />
    </RequiresAccess>
  );
};
