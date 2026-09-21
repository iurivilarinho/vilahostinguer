export type SettingsDto = {
  scanEnabled: boolean;
  scanIntervalSeconds: number;
  extraHosts: string[];
  autoSetup: boolean;
  backupDirectory: string;
  updatedAt: string;
};

export type SettingsRequest = Omit<SettingsDto, "updatedAt">;
