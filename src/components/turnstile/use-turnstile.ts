import { useCallback, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { TurnstileHandle } from "./turnstile-widget";

export type TurnstileConfig = {
  enabled: boolean;
  siteKey: string | null;
};

export const TURNSTILE_HEADER = "X-Turnstile-Token";

/** Header of a protected call; empty when the check is off or no token yet. */
export const turnstileHeaders = (token?: string | null): Record<string, string> =>
  token ? { [TURNSTILE_HEADER]: token } : {};

/**
 * State of the human check of one form. `loadConfig` reads the public route of the backend
 * (`{ enabled, siteKey }`), so turning Turnstile on needs no front build. Usage:
 *
 * ```tsx
 * const turnstile = useTurnstile(loadTurnstileConfig);
 * {turnstile.enabled && <TurnstileWidget {...turnstile.widgetProps} />}
 * <Button disabled={!turnstile.ready}>Entrar</Button>
 * // on submit: call({ ...values }, { headers: turnstileHeaders(turnstile.token) }); on error: turnstile.reset()
 * ```
 *
 * A token works once. A flow that makes two protected calls in a row (sign-up, then the automatic login) asks
 * `await turnstile.renew()` for a fresh token before the second call.
 */
export const useTurnstile = (loadConfig: () => Promise<TurnstileConfig>) => {
  const { data, isLoading } = useQuery({
    queryKey: ["turnstile-config"],
    queryFn: loadConfig,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
  const [token, setToken] = useState<string | null>(null);
  const widgetRef = useRef<TurnstileHandle>(null);
  const waitingRef = useRef<((fresh: string | null) => void)[]>([]);

  const siteKey = data?.siteKey ?? null;
  const enabled = data?.enabled === true && siteKey !== null;

  const reset = useCallback(() => {
    setToken(null);
    widgetRef.current?.reset();
  }, []);

  const receiveToken = useCallback((received: string | null) => {
    setToken(received);
    if (received) {
      const waiting = waitingRef.current;
      waitingRef.current = [];
      waiting.forEach((resolve) => resolve(received));
    }
  }, []);

  /** Asks for a new token and resolves with it (`null` when the check is off or no token came in time). */
  const renew = useCallback(
    (timeoutMs = 15_000): Promise<string | null> =>
      new Promise((resolve) => {
        if (!enabled) {
          resolve(null);
          return;
        }
        const timer = window.setTimeout(() => {
          waitingRef.current = waitingRef.current.filter((entry) => entry !== settle);
          resolve(null);
        }, timeoutMs);
        const settle = (fresh: string | null) => {
          window.clearTimeout(timer);
          resolve(fresh);
        };
        waitingRef.current.push(settle);
        setToken(null);
        widgetRef.current?.reset();
      }),
    [enabled],
  );

  return {
    enabled,
    token,
    /** True when the form may be sent: the check is off, or the token is in. */
    ready: !isLoading && (!enabled || token !== null),
    reset,
    renew,
    widgetProps: { siteKey: siteKey ?? "", onToken: receiveToken, ref: widgetRef },
  };
};
