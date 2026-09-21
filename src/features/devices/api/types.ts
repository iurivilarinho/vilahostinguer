import type { ApiRequestParams } from "@/lib/api/types";

export type DeviceStatus = "DISCOVERED" | "READY" | "AUTH_FAILED";
export type ConnectionType = "USB" | "NETWORK" | "VIRTUAL";
export type PackageManager = "APK" | "APT" | "DNF" | "PACMAN";
export type InitSystem = "SYSTEMD" | "OPENRC";
export type CredentialAuthType = "PASSWORD" | "PRIVATE_KEY";

export type CredentialBasicDto = {
  id: number;
  name: string;
  username: string;
  authType: CredentialAuthType;
};

export type DeviceBasicDto = {
  id: number;
  name: string;
  host: string;
  status: DeviceStatus;
  online: boolean;
};

export type DeviceDto = {
  id: number;
  name: string;
  host: string;
  port: number;
  hostKeyFingerprint: string;
  hostKeyType: string | null;
  connectionType: ConnectionType;
  connectionTypeDescription: string;
  interfaceName: string | null;
  status: DeviceStatus;
  statusDescription: string;
  online: boolean;
  lastSeenAt: string | null;
  credential: CredentialBasicDto | null;
  hostname: string | null;
  osName: string | null;
  osVersion: string | null;
  kernelVersion: string | null;
  architecture: string | null;
  cpuModel: string | null;
  cpuCores: number | null;
  memoryTotalBytes: number | null;
  diskTotalBytes: number | null;
  model: string | null;
  macAddress: string | null;
  packageManager: PackageManager | null;
  initSystem: InitSystem | null;
  homeDirectory: string | null;
  rootAccess: boolean;
  factsUpdatedAt: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type DeviceMetricsDto = {
  collectedAt: string;
  cpuPercent: number | null;
  load1: number | null;
  load5: number | null;
  load15: number | null;
  memoryTotalBytes: number | null;
  memoryUsedBytes: number | null;
  swapTotalBytes: number | null;
  swapUsedBytes: number | null;
  diskTotalBytes: number | null;
  diskUsedBytes: number | null;
  uptimeSeconds: number | null;
  batteryPercent: number | null;
  batteryStatus: string | null;
  temperatureCelsius: number | null;
  processCount: number | null;
  networkReceivedBytes: number | null;
  networkSentBytes: number | null;
};

export type DeviceFilter = {
  search?: string;
  status?: DeviceStatus[];
  online?: boolean;
  active?: boolean;
};

export type GetDevicesParams = ApiRequestParams<DeviceDto, DeviceFilter>;

export type DeviceRequest = {
  name: string;
  host: string;
  port: number;
  credentialId: number | null;
  notes: string | null;
};

export type UpdateDeviceRequest = DeviceRequest & { id: number };

export type ChangeDeviceActiveRequest = { id: number; active: boolean };

export type ScanStatusDto = {
  enabled: boolean;
  lastScanAt: string | null;
  usbInterfaces: string[];
  probedHosts: string[];
  reachableHosts: string[];
};

export type DeviceEventType = "DISCOVERED" | "ONLINE" | "OFFLINE" | "UPDATED" | "OPERATION_FINISHED" | "NETWORK_UPDATED";

export type DeviceEventDto = {
  type: DeviceEventType;
  deviceId: number | null;
  deviceName: string | null;
  operationId: number | null;
  message: string;
  occurredAt: string;
};
