import { RotasPortal } from "@/app/variables/rotas-portal";

/** Só volta para caminhos do próprio painel: um "?voltar=" nunca manda o cliente para outro site. */
export const safeReturnPath = (value: string | null): string =>
  value && value.startsWith("/") && !value.startsWith("//") ? value : RotasPortal.area;
