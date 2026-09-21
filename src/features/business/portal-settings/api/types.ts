export type CertificateStatus = "NONE" | "ISSUING" | "ACTIVE" | "FAILED";

export type PortalSettingsDto = {
  enabled: boolean;
  registrationOpen: boolean;
  companyName: string;
  hostname: string | null;
  customerSitesDomain: string | null;
  sshHost: string | null;
  portRangeStart: number;
  portRangeEnd: number;
  invoiceDaysBefore: number;
  suspendAfterDays: number;
  cancelAfterDays: number;
  mercadoPagoConfigured: boolean;
  manualPaymentInstructions: string | null;
  supportEmail: string | null;
  acmeEmail: string | null;
  certificateStatus: CertificateStatus;
  certificateStatusDescription: string;
  certificateMessage: string | null;
  certificateHostname: string | null;
  certificateExpiresAt: string | null;
  updatedAt: string;
};

export type PortalSettingsRequest = {
  enabled: boolean;
  registrationOpen: boolean;
  companyName: string;
  hostname: string;
  customerSitesDomain: string;
  sshHost: string;
  portRangeStart: number;
  portRangeEnd: number;
  invoiceDaysBefore: number;
  suspendAfterDays: number;
  cancelAfterDays: number;
  mercadoPagoAccessToken: string;
  removeMercadoPagoToken: boolean;
  manualPaymentInstructions: string;
  supportEmail: string | null;
  acmeEmail: string | null;
};
