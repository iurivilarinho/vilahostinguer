import { Globe, Network, RefreshCw, Router } from "lucide-react";
import { Badge, Button, Card, CardContent, CardHeader, StatCard, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, Typography } from "@/components";
import { formatRelative } from "@/lib/format";
import type { GatewayStatusDto } from "../api";
import { Notice } from "./notice";

type GatewayStatusPanelProps = {
  status: GatewayStatusDto;
  onRefreshUpnp: () => void;
  refreshingUpnp: boolean;
};

const routerValue = (status: GatewayStatusDto): string => {
  if (!status.upnp.enabled) {
    return "Manual";
  }
  return status.upnp.found ? (status.upnp.routerName ?? "Encontrado") : "Não encontrado";
};

const routerHint = (status: GatewayStatusDto): string => {
  if (!status.upnp.enabled) {
    return "UPnP desligado: redirecione as portas no roteador";
  }
  if (status.upnp.found) {
    return `${status.upnp.mappedPorts.length} porta(s) aberta(s) por UPnP`;
  }
  return status.upnp.error ?? "Procurando...";
};

/** Situação do gateway: IP público, portas abertas no PC, roteador e o passo a passo para liberar o acesso. */
export const GatewayStatusPanel = ({ status, onRefreshUpnp, refreshingUpnp }: GatewayStatusPanelProps) => {
  const listening = status.listeners.filter((listener) => listener.listening);
  const failing = status.listeners.filter((listener) => listener.error);
  const unmapped = listening.filter((listener) => !status.upnp.mappedPorts.includes(listener.port));
  const pcAddress = status.upnp.localAddress ?? status.lanAddresses[0] ?? "o IP deste PC";

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 md:grid-cols-3">
        <StatCard
          label="IP público"
          value={status.publicIp ?? "—"}
          icon={<Globe />}
          hint={status.publicIpError ?? "Como a internet vê a sua rede"}
          tone={status.publicIp ? "primary" : "warning"}
        />
        <StatCard
          label="Portas abertas no PC"
          value={`${listening.length}/${status.listeners.length}`}
          icon={<Network />}
          hint={status.listeners.length === 0 ? "Nenhuma rota ativa" : listening.map((listener) => listener.port).join(", ") || "Nenhuma abriu"}
          tone={failing.length > 0 ? "destructive" : "success"}
        />
        <StatCard label="Roteador" value={routerValue(status)} icon={<Router />} hint={routerHint(status)} tone={status.upnp.found ? "success" : "info"} />
      </div>

      {!status.enabled && (
        <Notice tone="info" title="Acesso remoto desligado">
          Nenhuma porta fica aberta no PC. Ligue nas preferências abaixo quando quiser publicar as rotas.
        </Notice>
      )}
      {status.cgnatSuspected && (
        <Notice tone="destructive" title="A operadora compartilha o seu IP (CGNAT)">
          {status.cgnatReason}
        </Notice>
      )}
      {failing.map((listener) => (
        <Notice key={listener.port} tone="warning" title={`Porta ${listener.port} fechada`}>
          {listener.error} Troque a porta nas preferências ou feche o programa que a usa.
        </Notice>
      ))}

      {listening.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex flex-col gap-1">
              <Typography variant="title-sm">Como liberar o acesso de fora</Typography>
              <Typography variant="caption">A conexão chega ao roteador, passa por este PC e segue até o dispositivo.</Typography>
            </div>
            {status.upnp.enabled && (
              <Button variant="outline" size="sm" onClick={onRefreshUpnp} loading={refreshingUpnp}>
                <RefreshCw />
                Procurar roteador
              </Button>
            )}
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <Typography variant="body-sm" as="p">
              <strong>1. Roteador.</strong>{" "}
              {unmapped.length === 0
                ? "O UPnP já abriu todas as portas no roteador."
                : `Crie um redirecionamento (port forwarding) TCP das portas ${unmapped.map((listener) => listener.port).join(", ")} para ${pcAddress}, nas mesmas portas. Vale reservar esse IP para o PC no DHCP do roteador.`}
            </Typography>
            <Typography variant="body-sm" as="p">
              <strong>2. Firewall do Windows.</strong> Na primeira vez em que uma porta abre, o Windows pergunta se o Bancada pode receber conexões: permita em
              redes privadas e públicas. Se negou antes, libere em Segurança do Windows → Firewall → Permitir um aplicativo.
            </Typography>
            <Typography variant="body-sm" as="p">
              <strong>3. Teste de fora.</strong> Use o 4G do celular: muitos roteadores não deixam um aparelho da própria casa acessar o IP público (falta de
              “NAT loopback”).
            </Typography>
          </CardContent>
        </Card>
      )}

      {status.listeners.length > 0 && (
        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Porta no PC</TableHead>
                <TableHead>Recebe</TableHead>
                <TableHead>Rotas</TableHead>
                <TableHead>Roteador</TableHead>
                <TableHead>Situação</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {status.listeners.map((listener) => (
                <TableRow key={listener.port}>
                  <TableCell>
                    <Typography variant="mono">{listener.port}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body-sm">{listener.typeDescription}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body-sm">{listener.routeCount}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption">
                      {status.upnp.mappedPorts.includes(listener.port) ? "Aberta por UPnP" : "Redirecionar à mão"}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {listener.listening ? <Badge tone="success">Aberta</Badge> : <Badge tone="destructive">Fechada</Badge>}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}

      {status.upnp.checkedAt && (
        <Typography variant="caption" as="p">
          Roteador verificado {formatRelative(status.upnp.checkedAt)}.
        </Typography>
      )}
    </div>
  );
};
