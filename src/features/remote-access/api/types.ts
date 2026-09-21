import type { DeviceBasicDto } from "@/features/devices/api";
import type { MachineNetworkMode, MachineStatus } from "@/features/machines/api";
import type { ApiRequestParams } from "@/lib/api/types";

export type DnsProvider = "CLOUDFLARE" | "DUCKDNS" | "CUSTOM_URL" | "MANUAL";
export type DdnsSyncResult = "PENDING" | "SYNCED" | "FAILED";
export type RouteType = "HTTP" | "TLS" | "TCP";
export type RouteStatus = "ACTIVE" | "PAUSED" | "REMOVED";

export type DomainDto = {
  id: number;
  name: string;
  provider: DnsProvider;
  providerDescription: string;
  hasSecret: boolean;
  wildcard: boolean;
  ddnsEnabled: boolean;
  intervalMinutes: number;
  lastIp: string | null;
  resolvedIp: string | null;
  lastSyncAt: string | null;
  lastResult: DdnsSyncResult;
  lastResultDescription: string;
  lastMessage: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type DomainBasicDto = {
  id: number;
  name: string;
};

export type DomainFilter = {
  search?: string;
  provider?: DnsProvider[];
  active?: boolean;
};

export type GetDomainsParams = ApiRequestParams<DomainDto, DomainFilter>;

export type DomainRequest = {
  name: string;
  provider: DnsProvider;
  secret: string;
  wildcard: boolean;
  ddnsEnabled: boolean;
  intervalMinutes: number;
};

export type UpdateDomainRequest = DomainRequest & { id: number };

export type ChangeDomainActiveRequest = {
  id: number;
  active: boolean;
};

export type MachineBasicDto = {
  id: number;
  name: string;
  networkMode: MachineNetworkMode;
  status: MachineStatus;
};

export type RouteDto = {
  id: number;
  type: RouteType;
  typeDescription: string;
  hostname: string | null;
  publicPort: number | null;
  device: DeviceBasicDto;
  machine: MachineBasicDto | null;
  domain: DomainBasicDto | null;
  targetPort: number;
  description: string | null;
  status: RouteStatus;
  statusDescription: string;
  createdAt: string;
  updatedAt: string;
};

export type RouteFilter = {
  search?: string;
  type?: RouteType[];
  status?: RouteStatus[];
  deviceId?: number;
  machineId?: number;
  domainId?: number;
};

export type GetRoutesParams = ApiRequestParams<RouteDto, RouteFilter>;

export type RouteRequest = {
  type: RouteType;
  hostname: string | null;
  publicPort: number | null;
  deviceId: number | null;
  machineId: number | null;
  targetPort: number;
  description: string | null;
};

export type UpdateRouteRequest = RouteRequest & { id: number };

export type ChangeRouteStatusRequest = {
  id: number;
  status: RouteStatus;
};

export type GatewayListenerDto = {
  port: number;
  type: RouteType;
  typeDescription: string;
  listening: boolean;
  error: string | null;
  routeCount: number;
};

export type RouteTrafficDto = {
  routeId: number;
  activeConnections: number;
  totalConnections: number;
  bytesIn: number;
  bytesOut: number;
  lastConnectionAt: string | null;
  lastError: string | null;
  lastErrorAt: string | null;
};

export type UpnpStatusDto = {
  enabled: boolean;
  found: boolean;
  routerName: string | null;
  localAddress: string | null;
  externalIp: string | null;
  mappedPorts: number[];
  error: string | null;
  checkedAt: string | null;
};

export type GatewayStatusDto = {
  enabled: boolean;
  httpPort: number;
  tlsPort: number;
  publicIp: string | null;
  publicIpError: string | null;
  lanAddresses: string[];
  cgnatSuspected: boolean;
  cgnatReason: string | null;
  listeners: GatewayListenerDto[];
  traffic: RouteTrafficDto[];
  upnp: UpnpStatusDto;
};

export type GatewaySettingsRequest = {
  enabled: boolean;
  httpPort: number;
  tlsPort: number;
  upnpEnabled: boolean;
};
