export type DashboardSummaryDto = {
  totalDevices: number;
  onlineDevices: number;
  devicesNeedingAttention: number;
  runningOperations: number;
  failedOperationsLastDay: number;
  availableBackups: number;
  lastBackupAt: string | null;
};
