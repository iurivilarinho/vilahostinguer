import type { ApiRequestParams } from "@/lib/api/types";
import type { CredentialAuthType } from "@/features/devices/api";

export type CredentialDto = {
  id: number;
  name: string;
  username: string;
  authType: CredentialAuthType;
  authTypeDescription: string;
  hasPassphrase: boolean;
  defaultCredential: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CredentialSecretDto = {
  id: number;
  secret: string | null;
  passphrase: string | null;
};

export type CredentialFilter = {
  search?: string;
  active?: boolean;
};

export type GetCredentialsParams = ApiRequestParams<CredentialDto, CredentialFilter>;

export type CredentialRequest = {
  name: string;
  username: string;
  authType: CredentialAuthType;
  secret: string;
  passphrase: string;
  defaultCredential: boolean;
};

export type UpdateCredentialRequest = CredentialRequest & { id: number };

export type ChangeCredentialActiveRequest = { id: number; active: boolean };
