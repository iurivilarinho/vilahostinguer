import type { MachineAction, MachineDistribution, MachineStatus } from "@/features/machines/api";
import type { SubscriptionStatus } from "../../billing/api";

export type ServerDto = {
  id: number;
  hostname: string;
  planName: string;
  cpuLimit: number;
  memoryMb: number;
  diskGb: number;
  backupSlots: number;
  distribution: MachineDistribution;
  distributionName: string;
  version: string;
  username: string;
  subscriptionStatus: SubscriptionStatus;
  subscriptionStatusDescription: string;
  machineStatus: MachineStatus | null;
  machineStatusDescription: string | null;
  sshHost: string | null;
  sshPort: number | null;
  sshCommand: string | null;
  siteHostname: string | null;
  nextDueDate: string | null;
  cancelAtPeriodEnd: boolean;
  notice: string | null;
};

export type ServerStatsDto = {
  machineId: number;
  cpuPercent: number | null;
  memoryUsage: string | null;
  memoryPercent: number | null;
  processCount: number | null;
};

export type ServerBackupStatus = "CREATING" | "AVAILABLE" | "FAILED" | "DISCARDED";

export type ServerBackupDto = {
  id: number;
  name: string;
  sizeBytes: number | null;
  status: ServerBackupStatus;
  statusDescription: string;
  operationId: number | null;
  createdAt: string;
};

export type ServerOperationStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELED";

export type ServerOperationDto = {
  id: number;
  title: string;
  status: ServerOperationStatus;
  statusDescription: string;
  finished: boolean;
  log: string;
  startedAt: string | null;
  finishedAt: string | null;
};

export type ServerActionRequest = {
  id: number;
  action: MachineAction;
};

export type ServerReinstallRequest = {
  id: number;
  distribution: MachineDistribution;
  version: string;
  password: string;
  backupFirst: boolean;
};

export type ServerPasswordRequest = {
  id: number;
  password: string;
};

export type ServerBackupRequest = {
  id: number;
  name: string;
};

export type ServerBackupActionRequest = {
  id: number;
  backupId: number;
};
