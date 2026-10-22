/** Minimal typing of the Cloudflare Turnstile browser API (only what the widget uses). */
export type TurnstileRenderOptions = {
  sitekey: string;
  language?: string;
  theme?: "auto" | "light" | "dark";
  appearance?: "always" | "execute" | "interaction-only";
  size?: "normal" | "flexible" | "compact";
  callback?: (token: string) => void;
  "expired-callback"?: () => void;
  "error-callback"?: () => void;
};

export type TurnstileApi = {
  render: (container: HTMLElement, options: TurnstileRenderOptions) => string;
  reset: (widgetId?: string) => void;
  remove: (widgetId?: string) => void;
};

declare global {
  interface Window {
    turnstile?: TurnstileApi;
  }
}

export const TURNSTILE_SCRIPT_URL = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";

let scriptPromise: Promise<TurnstileApi> | null = null;

/** Loads the Cloudflare script once per page; a failed load can be tried again by the next call. */
export const loadTurnstile = (): Promise<TurnstileApi> => {
  if (window.turnstile) return Promise.resolve(window.turnstile);
  if (scriptPromise) return scriptPromise;
  scriptPromise = new Promise<TurnstileApi>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = TURNSTILE_SCRIPT_URL;
    script.async = true;
    script.defer = true;
    script.onload = () => (window.turnstile ? resolve(window.turnstile) : reject(new Error("turnstile-missing")));
    script.onerror = () => {
      script.remove();
      reject(new Error("turnstile-blocked"));
    };
    document.head.appendChild(script);
  }).catch((error: unknown) => {
    scriptPromise = null;
    throw error;
  });
  return scriptPromise;
};
