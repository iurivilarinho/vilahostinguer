export type CatalogAppKey =
  | "NGINX"
  | "CADDY"
  | "NODEJS"
  | "JAVA17"
  | "JAVA21"
  | "PYTHON"
  | "POSTGRESQL"
  | "MARIADB"
  | "REDIS"
  | "DOCKER"
  | "GIT"
  | "CERTBOT"
  | "ESSENTIALS";

export type AppCategory = "WEB_SERVER" | "RUNTIME" | "DATABASE" | "CONTAINER" | "TOOL";

export type ServiceAction = "START" | "STOP" | "RESTART" | "ENABLE" | "DISABLE";

export type DeviceAppDto = {
  key: CatalogAppKey;
  name: string;
  description: string;
  category: AppCategory;
  categoryDescription: string;
  available: boolean;
  installed: boolean;
  version: string | null;
  serviceName: string | null;
  serviceRunning: boolean;
  serviceEnabled: boolean;
};

export type AppActionRequest = {
  deviceId: number;
  app: CatalogAppKey;
};

export type AppServiceActionRequest = AppActionRequest & { action: ServiceAction };
