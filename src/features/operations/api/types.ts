import type { ApiRequestParams } from "@/lib/api/types";
import type { DeviceBasicDto } from "@/features/devices/api";

export type OperationType = "INSTALL_APP" | "REMOVE_APP" | "SERVICE_ACTION" | "SYSTEM_UPGRADE" | "BACKUP" | "RESTORE" | "FORMAT_PARTITION" | "MACHINE_CREATE" | "MACHINE_ACTION" | "MACHINE_REMOVE";
export type OperationStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELED";

export type OperationBasicDto = {
  id: number;
  device: DeviceBasicDto;
  type: OperationType;
  typeDescription: string;
  status: OperationStatus;
  statusDescription: string;
  title: string;
  target: string | null;
  exitCode: number | null;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
};

export type OperationDto = OperationBasicDto & {
  finished: boolean;
  log: string;
  updatedAt: string;
};

export type OperationFilter = {
  deviceId?: number;
  type?: OperationType[];
  status?: OperationStatus[];
  startDate?: string;
  endDate?: string;
};

export type GetOperationsParams = ApiRequestParams<OperationBasicDto, OperationFilter>;
