export type DeviceSection =
  | "visao-geral"
  | "terminal"
  | "aplicativos"
  | "maquinas"
  | "arquivos"
  | "backups"
  | "armazenamento"
  | "atividades"
  | "configuracoes";

export const Rotas = {
  dashboard: "/",
  devices: {
    list: "/dispositivos",
    detail: (id: number | string) => `/dispositivos/${id}`,
    section: (id: number | string, section: DeviceSection) => `/dispositivos/${id}/${section}`,
  },
  terminal: "/terminal",
  machines: "/maquinas",
  backups: "/backups",
  operations: "/atividades",
  credentials: "/credenciais",
  settings: "/configuracoes",
  designSystem: "/design-system",
} as const;
