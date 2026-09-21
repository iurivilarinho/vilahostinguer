const STORAGE_KEY = "bancada.token";
const QUERY_PARAM = "token";

/**
 * Token de sessão gerado pela casca Tauri a cada abertura. Ele chega uma vez na URL
 * (`?token=...`), vai para o sessionStorage e some da barra de endereço.
 */
export const tokenStorage = {
  captureFromUrl: () => {
    const url = new URL(window.location.href);
    const token = url.searchParams.get(QUERY_PARAM);
    if (!token) {
      return;
    }
    sessionStorage.setItem(STORAGE_KEY, token);
    url.searchParams.delete(QUERY_PARAM);
    window.history.replaceState(window.history.state, "", url.pathname + url.search + url.hash);
  },
  get: (): string | null => sessionStorage.getItem(STORAGE_KEY),
  /** Anexa o token a URLs que não aceitam cabeçalho (EventSource, WebSocket, download direto). */
  withToken: (path: string): string => {
    const token = sessionStorage.getItem(STORAGE_KEY);
    if (!token) {
      return path;
    }
    const separator = path.includes("?") ? "&" : "?";
    return `${path}${separator}${QUERY_PARAM}=${encodeURIComponent(token)}`;
  },
};
