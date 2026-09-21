import axios from "axios";
import { tokenStorage } from "@/app/utils/token-storage";

const REQUEST_TIMEOUT_MS = 120_000;

/**
 * Cliente único da API. Arrays viram parâmetros repetidos (`status=A&status=B`), que é o
 * formato que o Spring liga nos objetos de filtro.
 */
export const api = axios.create({
  baseURL: "/api",
  timeout: REQUEST_TIMEOUT_MS,
  paramsSerializer: { indexes: null },
});

api.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) {
    config.headers.set("X-Bancada-Token", token);
  }
  return config;
});

/**
 * Cliente do painel do cliente. A sessão vai no cookie httpOnly `token`, que o navegador manda
 * sozinho; nenhum token passa pelo JavaScript.
 */
export const portalApi = axios.create({
  baseURL: "/api/portal",
  timeout: REQUEST_TIMEOUT_MS,
  withCredentials: true,
  paramsSerializer: { indexes: null },
});
