import axios from "axios";
import type { ApiErrorBody } from "@/lib/api/types";

/** Mensagem legível a partir do erro da API (`{ timestamp, message: [] }`). Nunca mostrar o erro cru. */
export const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const messages = error.response?.data?.message;
    if (messages?.length) {
      return messages.join(" ");
    }
    if (!error.response) {
      return "O painel não respondeu. Verifique se o Bancada está aberto.";
    }
  }
  return fallback;
};
