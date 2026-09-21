import type { DistributionDto } from "@/features/machines/api";

export type BillingCycle = "MONTHLY" | "QUARTERLY" | "SEMIANNUAL" | "ANNUAL";

export type PortalInfoDto = {
  companyName: string;
  registrationOpen: boolean;
  automaticPix: boolean;
  manualPaymentInstructions: string | null;
  supportEmail: string | null;
};

export type PlanPriceDto = {
  cycle: BillingCycle;
  cycleDescription: string;
  months: number;
  discountPercent: number;
  total: number;
  perMonth: number;
};

export type PortalPlanDto = {
  id: number;
  name: string;
  description: string | null;
  cpuLimit: number;
  memoryMb: number;
  diskGb: number;
  backupSlots: number;
  prices: PlanPriceDto[];
  featured: boolean;
  available: boolean;
  distributions: DistributionDto[];
};
