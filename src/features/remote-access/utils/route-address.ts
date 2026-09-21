import type { GatewayStatusDto, RouteDto } from "../api";

const DEFAULT_HTTP_PORT = 80;
const DEFAULT_HTTPS_PORT = 443;

/**
 * Endereço que um visitante de fora usa para chegar à rota: o nome do site (com a porta, se o
 * gateway não estiver nas portas padrão) ou host:porta para TCP, pelo domínio ou pelo IP público.
 */
export const routeAddress = (route: RouteDto, gateway?: GatewayStatusDto): string => {
  if (route.type === "HTTP") {
    const port = gateway?.httpPort ?? DEFAULT_HTTP_PORT;
    return `http://${route.hostname}${port === DEFAULT_HTTP_PORT ? "" : `:${port}`}`;
  }
  if (route.type === "TLS") {
    const port = gateway?.tlsPort ?? DEFAULT_HTTPS_PORT;
    return `https://${route.hostname}${port === DEFAULT_HTTPS_PORT ? "" : `:${port}`}`;
  }
  const host = gateway?.publicIp ?? "ip-público";
  return `${host}:${route.publicPort}`;
};

/** Destino legível: máquina (ou dispositivo) e porta do serviço. */
export const routeTargetLabel = (route: RouteDto): string =>
  route.machine ? `${route.machine.name} (${route.device.name}) :${route.targetPort}` : `${route.device.name} :${route.targetPort}`;
