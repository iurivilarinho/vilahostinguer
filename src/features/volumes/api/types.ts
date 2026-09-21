import type { DeviceBasicDto } from "@/features/devices/api";
import type { MachineNetworkMode, MachineStatus } from "@/features/machines/api";
import type { ApiRequestParams } from "@/lib/api/types";

export type VolumeStatus = "AVAILABLE" | "ATTACHING" | "ATTACHED" | "WAITING" | "DETACHING" | "FAILED" | "DELETED";

export type VolumeMachineDto = {
  id: number;
  name: string;
  networkMode: MachineNetworkMode;
  status: MachineStatus;
};

export type VolumeDto = {
  id: number;
  name: string;
  drive: string;
  filePath: string;
  sizeBytes: number;
  writtenBytes: number;
  status: VolumeStatus;
  statusDescription: string;
  statusMessage?: string | null;
  device?: DeviceBasicDto | null;
  mountPath?: string | null;
  machine?: VolumeMachineDto | null;
  containerPath?: string | null;
  formatted: boolean;
  connectedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type HostDiskDto = {
  root: string;
  label: string;
  fileSystem: string;
  totalBytes: number;
  freeBytes: number;
  allocatedBytes: number;
  reservedBytes: number;
  availableBytes: number;
  removable: boolean;
  supported: boolean;
  unsupportedReason?: string | null;
};

export type VolumeServerDto = {
  port: number;
  running: boolean;
  error?: string | null;
  disks: HostDiskDto[];
};

export type VolumeFilter = {
  search?: string;
  status?: VolumeStatus[];
  deviceId?: number;
  machineId?: number;
  drive?: string;
};

export type GetVolumesParams = ApiRequestParams<VolumeDto, VolumeFilter>;

export type VolumeRequest = {
  name: string;
  drive: string;
  sizeGb: number;
};

export type VolumeAttachRequest = {
  id: number;
  deviceId?: number;
  machineId?: number;
  mountPath?: string;
  containerPath?: string;
};

export type VolumeDeleteRequest = {
  id: number;
  confirmation: string;
};
