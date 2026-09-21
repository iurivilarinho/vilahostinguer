export type ServerSection = "visao-geral" | "terminal" | "sistema" | "backups" | "configuracoes";

/** Rotas do painel do cliente (portal.html, publicado no domínio da empresa). */
export const RotasPortal = {
  home: "/",
  login: "/entrar",
  register: "/cadastro",
  area: "/painel",
  servers: "/painel/vps",
  server: (id: number | string) => `/painel/vps/${id}`,
  serverSection: (id: number | string, section: ServerSection) => `/painel/vps/${id}/${section}`,
  checkout: (planId: number | string) => `/painel/contratar/${planId}`,
  billing: "/painel/faturamento",
  invoice: (id: number | string) => `/painel/faturas/${id}`,
  account: "/painel/conta",
} as const;
