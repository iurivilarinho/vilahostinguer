export type CustomerStatus = "ACTIVE" | "SUSPENDED" | "CLOSED";

export type PortalCustomerDto = {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  document: string | null;
  status: CustomerStatus;
  statusDescription: string;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  name: string;
  email: string;
  password: string;
  phone: string;
  document: string;
  acceptTerms: boolean;
};

export type ProfileRequest = {
  name: string;
  email: string;
  phone: string;
  document: string;
};

export type ChangePasswordRequest = {
  currentPassword: string;
  newPassword: string;
};
