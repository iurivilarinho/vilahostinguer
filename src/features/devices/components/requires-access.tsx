import { KeyRound, PlugZap } from "lucide-react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Button, Card, EmptyState } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import type { DeviceDto } from "../api";

type RequiresAccessProps = {
  device: DeviceDto;
  children: ReactNode;
  requireOnline?: boolean;
};

/** Seções que entram no dispositivo só aparecem quando há credencial funcionando (e, se pedido, conexão). */
export const RequiresAccess = ({ device, children, requireOnline = true }: RequiresAccessProps) => {
  if (device.status !== "READY") {
    return (
      <Card>
        <EmptyState
          icon={<KeyRound />}
          title={device.status === "AUTH_FAILED" ? "O dispositivo recusou a credencial" : "Falta uma credencial"}
          description="Vincule um usuário e senha (ou chave) que funcione neste dispositivo para liberar terminal, aplicativos, arquivos e backups."
          action={
            <Link to={Rotas.devices.section(device.id, "configuracoes")}>
              <Button>Configurar acesso</Button>
            </Link>
          }
        />
      </Card>
    );
  }
  if (requireOnline && !device.online) {
    return (
      <Card>
        <EmptyState
          icon={<PlugZap />}
          title="Dispositivo desconectado"
          description="Reconecte o cabo ou verifique a rede. Esta tela volta a funcionar assim que ele responder."
        />
      </Card>
    );
  }
  return <>{children}</>;
};
