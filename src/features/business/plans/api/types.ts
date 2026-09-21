import type { DeviceBasicDto } from "@/features/devices/api";
import type { PlanPriceDto } from "@/features/portal/catalog/api";
import type { ApiRequestParams } from "@/lib/api/types";

export type PlanDto = {
  id: number;
  name: string;
  description: string | null;
  device: DeviceBasicDto;
  cpuLimit: number;
  memoryMb: number;
  diskGb: number;
  backupSlots: number;
  priceMonthly: number;
  prices: PlanPriceDto[];
  active: boolean;
  featured: boolean;
  orderNumber: number;
  available: boolean;
  subscriptionCount: number;
  createdAt: string;
  updatedAt: string;
};

export type PlanFilter = {
  search?: string;
  active?: boolean;
  deviceId?: number;
};

export type GetPlansParams = ApiRequestParams<PlanDto, PlanFilter>;

export type PlanRequest = {
  name: string;
  description: string;
  deviceId: number;
  cpuLimit: number;
  memoryMb: number;
  diskGb: number;
  backupSlots: number;
  priceMonthly: number;
  active: boolean;
  featured: boolean;
  orderNumber: number;
};

export type UpdatePlanRequest = PlanRequest & { id: number };
