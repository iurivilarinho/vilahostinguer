import type { DeviceBasicDto } from "@/features/devices/api";
import type { ApiRequestParams } from "@/lib/api/types";

export type MachineDistribution = "UBUNTU" | "DEBIAN" | "ALPINE" | "FEDORA" | "ROCKY" | "ARCH";
export type MachineStatus = "CREATING" | "RUNNING" | "STOPPED" | "FAILED" | "REMOVED";
export type MachineNetworkMode = "BRIDGE" | "HOST";
export type MachineAction = "START" | "STOP" | "RESTART";

export type MachinePortDto = {
  hostPort: number;
  containerPort: number;
  protocol: string;
};

export type MachineVolumeDto = {
  hostPath: string;
  containerPath: string;
};

export type MachineDto = {
  id: number;
  device: DeviceBasicDto;
  name: string;
  containerName: string;
  distribution: MachineDistribution;
  distributionName: string;
  version: string;
  image: string;
  cpuLimit: number | null;
  memoryLimitMb: number | null;
  networkMode: MachineNetworkMode;
  networkModeDescription: string;
  ports: MachinePortDto[];
  volumes: MachineVolumeDto[];
  username: string;
  sshEnabled: boolean;
  sshPort: number | null;
  autoStart: boolean;
  status: MachineStatus;
  statusDescription: string;
  createdAt: string;
  updatedAt: string;
};

export type MachineFilter = {
  deviceId?: number;
  search?: string;
  status?: MachineStatus[];
};

export type GetMachinesParams = ApiRequestParams<MachineDto, MachineFilter>;

export type MachineRequest = {
  deviceId: number;
  name: string;
  distribution: MachineDistribution;
  version: string;
  cpuLimit: number | null;
  memoryLimitMb: number | null;
  networkMode: MachineNetworkMode;
  ports: MachinePortDto[];
  volumes: MachineVolumeDto[];
  username: string;
  password: string;
  installSsh: boolean;
  sshPort: number | null;
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
  memoryUsage: string;
  memoryPercent: number | null;
  processCount: number | null;
};

export type DockerStatusDto = {
  installed: boolean;
  running: boolean;
  version: string | null;
  missingKernelFeatures: string[];
  message: string | null;
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
