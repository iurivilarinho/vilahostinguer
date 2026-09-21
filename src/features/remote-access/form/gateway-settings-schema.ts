import { z } from "zod";

const port = z.coerce.number({ invalid_type_error: "Informe a porta" }).int("Porta inválida").min(1, "Porta inválida").max(65_535, "Porta inválida");

export const gatewaySettingsSchema = z
  .object({
    enabled: z.boolean(),
    httpPort: port,
    tlsPort: port,
    upnpEnabled: z.boolean(),
  })
  .refine((values) => values.httpPort !== values.tlsPort, { path: ["tlsPort"], message: "Use uma porta diferente da HTTP" });

export type GatewaySettingsFormValues = z.infer<typeof gatewaySettingsSchema>;

export const DEFAULT_GATEWAY_SETTINGS_VALUES: GatewaySettingsFormValues = {
  enabled: true,
  httpPort: 80,
  tlsPort: 443,
  upnpEnabled: false,
};
