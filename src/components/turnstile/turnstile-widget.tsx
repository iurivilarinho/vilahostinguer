import { useEffect, useImperativeHandle, useRef, useState, type Ref } from "react";
import { loadTurnstile, type TurnstileApi } from "./turnstile-script";

export type TurnstileHandle = {
  /** Asks for a new token. Each token works once, so call it after a submit that failed. */
  reset: () => void;
};

export type TurnstileWidgetProps = {
  siteKey: string;
  /** Receives the token when the check passes, and `null` when it expires or fails. */
  onToken: (token: string | null) => void;
  ref?: Ref<TurnstileHandle>;
  className?: string;
};

const LOAD_ERROR =
  "Não foi possível carregar a verificação de segurança. Desative o bloqueador de anúncios e recarregue a página.";
const CHECK_ERROR = "A verificação de segurança falhou. Recarregue a página e tente de novo.";

/**
 * Cloudflare Turnstile in managed mode (PLT-033, padronização 032): the widget only shows itself
 * when the visitor has to interact, in PT-BR and following the light/dark theme. The token is
 * reported through `onToken`; send it in the `X-Turnstile-Token` header of the protected call.
 */
export const TurnstileWidget = ({ siteKey, onToken, ref, className }: TurnstileWidgetProps) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const apiRef = useRef<TurnstileApi | null>(null);
  const widgetIdRef = useRef<string | null>(null);
  const onTokenRef = useRef(onToken);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    onTokenRef.current = onToken;
  }, [onToken]);

  useImperativeHandle(ref, () => ({
    reset: () => {
      if (apiRef.current && widgetIdRef.current) apiRef.current.reset(widgetIdRef.current);
    },
  }));

  useEffect(() => {
    let cancelled = false;
    const container = containerRef.current;
    loadTurnstile()
      .then((api) => {
        if (cancelled || !container) return;
        apiRef.current = api;
        widgetIdRef.current = api.render(container, {
          sitekey: siteKey,
          language: "pt-br",
          theme: "auto",
          appearance: "interaction-only",
          size: "flexible",
          callback: (token) => {
            setError(null);
            onTokenRef.current(token);
          },
          "expired-callback": () => onTokenRef.current(null),
          "error-callback": () => {
            onTokenRef.current(null);
            setError(CHECK_ERROR);
          },
        });
      })
      .catch(() => {
        if (!cancelled) setError(LOAD_ERROR);
      });
    return () => {
      cancelled = true;
      if (apiRef.current && widgetIdRef.current) apiRef.current.remove(widgetIdRef.current);
      widgetIdRef.current = null;
    };
  }, [siteKey]);

  return (
    <div className={className}>
      <div ref={containerRef} data-testid="turnstile-container" />
      {error && (
        <p role="alert" className="mt-2 text-sm text-destructive">
          {error}
        </p>
      )}
    </div>
  );
};
