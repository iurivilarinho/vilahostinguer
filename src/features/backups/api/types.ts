import type { ApiRequestParams } from "@/lib/api/types";
import type { DeviceBasicDto } from "@/features/devices/api";

export type BackupStatus = "CREATING" | "AVAILABLE" | "FAILED" | "DISCARDED";

export type BackupDto = {
  id: number;
  device: DeviceBasicDto;
  operationId: number | null;
  name: string;
  paths: string[];
  filePath: string | null;
  sizeBytes: number | null;
  sha256: string | null;
  status: BackupStatus;
  statusDescription: string;
  createdAt: string;
  updatedAt: string;
};

export type BackupFilter = {
  deviceId?: number;
  search?: string;
  status?: BackupStatus[];
};

export type GetBackupsParams = ApiRequestParams<BackupDto, BackupFilter>;

export type BackupRequest = {
  deviceId: number;
  name: string;
  paths: string[];
};
