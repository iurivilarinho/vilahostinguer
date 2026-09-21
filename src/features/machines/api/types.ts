import type { DeviceBasicDto } from "@/features/devices/api";
import type { HostDiskDto } from "@/features/volumes/api";
import type { ApiRequestParams } from "@/lib/api/types";

export type MachineDistribution = "UBUNTU" | "DEBIAN" | "ROCKY" | "ALMA";
export type MachineStatus = "CREATING" | "RUNNING" | "STOPPED" | "FAILED" | "REMOVED";
export type MachineAction = "START" | "STOP" | "RESTART";

/** Máquina virtual Linux deste PC (Hyper-V); o `device` é ela vista como dispositivo do painel. */
export type MachineDto = {
  id: number;
  device: DeviceBasicDto | null;
  name: string;
  vmName: string;
  distribution: MachineDistribution;
  distributionName: string;
  version: string;
  cpuCount: number;
  memoryMb: number;
  diskGb: number;
  drive: string;
  ipAddress: string;
  username: string;
  autoStart: boolean;
  status: MachineStatus;
  statusDescription: string;
  createdAt: string;
  updatedAt: string;
};

export type MachineFilter = {
  search?: string;
  status?: MachineStatus[];
};

export type GetMachinesParams = ApiRequestParams<MachineDto, MachineFilter>;

export type MachineRequest = {
  name: string;
  distribution: MachineDistribution;
  version: string;
  cpuCount: number;
  memoryMb: number;
  diskGb: number;
  drive?: string;
  username: string;
  password: string;
  autoStart: boolean;
};

export type MachineCreationDto = {
  machine: MachineDto;
  operationId: number;
};

export type MachineActionRequest = {
  id: number;
  action: MachineAction;
};

export type MachineBackupRequest = {
  id: number;
  name: string;
};

export type MachineRestoreRequest = {
  id: number;
  backupId: number;
};

export type MachineReinstallRequest = {
  id: number;
  distribution: MachineDistribution;
  version: string;
  password: string;
  backupFirst: boolean;
};

export type MachineStatsDto = {
  machineId: number;
  cpuPercent: number | null;
  memoryUsage: string | null;
  memoryPercent: number | null;
  processCount: number | null;
};

export type DistributionDto = {
  key: MachineDistribution;
  name: string;
  versions: string[];
  supported: boolean;
};

export type MachineLogsDto = {
  machineId: number;
  log: string;
};

/** Este PC como anfitrião das máquinas. */
export type MachineHostDto = {
  hyperVInstalled: boolean;
  hyperVPermitted: boolean;
  networkReady: boolean;
  ready: boolean;
  message: string | null;
  switchName: string;
  network: string;
  cpus: number;
  memoryMb: number;
  reservedMemoryMb: number;
  usedMemoryMb: number;
  availableMemoryMb: number;
  machineCount: number;
  disks: HostDiskDto[];
};
