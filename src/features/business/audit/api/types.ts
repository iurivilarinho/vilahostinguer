import type { ApiRequestParams } from "@/lib/api/types";

export type AuditAction =
  | "CREATE"
  | "UPDATE"
  | "STATUS_CHANGE"
  | "PAYMENT"
  | "PROVISION"
  | "PASSWORD_CHANGE"
  | "SERVER_ACTION"
  | "REINSTALL"
  | "BACKUP"
  | "RESTORE";

export type AuditLogDto = {
  id: number;
  entityType: string;
  entityId: number | null;
  action: AuditAction;
  actionDescription: string;
  userId: number | null;
  userName: string | null;
  occurredAt: string;
  oldValue: string | null;
  newValue: string | null;
  reason: string | null;
  source: string | null;
};

export type AuditLogFilter = {
  entityType?: string;
  entityId?: number;
  userId?: number;
  action?: AuditAction[];
};

export type GetAuditLogsParams = ApiRequestParams<AuditLogDto, AuditLogFilter>;
