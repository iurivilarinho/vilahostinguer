import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, render, renderHook, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { TurnstileApi, TurnstileRenderOptions } from "./turnstile-script";
import { TurnstileWidget, type TurnstileHandle } from "./turnstile-widget";
import { turnstileHeaders, useTurnstile } from "./use-turnstile";

const installFakeApi = () => {
  let options: TurnstileRenderOptions | undefined;
  const api: TurnstileApi = {
    render: vi.fn((_container: HTMLElement, renderOptions: TurnstileRenderOptions) => {
      options = renderOptions;
      return "widget-1";
    }),
    reset: vi.fn(),
    remove: vi.fn(),
  };
  window.turnstile = api;
  return { api, options: () => options as TurnstileRenderOptions };
};

afterEach(() => {
  delete window.turnstile;
});

describe("TurnstileWidget", () => {
  it("renders in managed mode, PT-BR, following the theme, and reports the token", async () => {
    const fake = installFakeApi();
    const onToken = vi.fn();
    render(<TurnstileWidget siteKey="site-key" onToken={onToken} />);

    await waitFor(() => expect(fake.api.render).toHaveBeenCalledTimes(1));
    expect(fake.options()).toMatchObject({
      sitekey: "site-key",
      language: "pt-br",
      theme: "auto",
      appearance: "interaction-only",
    });

    act(() => fake.options().callback?.("token-abc"));
    expect(onToken).toHaveBeenLastCalledWith("token-abc");
    act(() => fake.options()["expired-callback"]?.());
    expect(onToken).toHaveBeenLastCalledWith(null);
  });

  it("resets the widget on demand and removes it on unmount", async () => {
    const fake = installFakeApi();
    const ref = { current: null as TurnstileHandle | null };
    const { unmount } = render(<TurnstileWidget siteKey="k" onToken={() => {}} ref={ref} />);
    await waitFor(() => expect(fake.api.render).toHaveBeenCalled());

    ref.current?.reset();
    expect(fake.api.reset).toHaveBeenCalledWith("widget-1");
    unmount();
    expect(fake.api.remove).toHaveBeenCalledWith("widget-1");
  });

  it("tells the visitor when Cloudflare refuses the check", async () => {
    const fake = installFakeApi();
    render(<TurnstileWidget siteKey="k" onToken={() => {}} />);
    await waitFor(() => expect(fake.api.render).toHaveBeenCalled());

    act(() => fake.options()["error-callback"]?.());
    expect((await screen.findByRole("alert")).textContent).toContain("A verificação de segurança falhou");
  });
});

const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>
);

describe("useTurnstile", () => {
  it("is ready at once when the check is off", async () => {
    const { result } = renderHook(() => useTurnstile(async () => ({ enabled: false, siteKey: null })), { wrapper });
    await waitFor(() => expect(result.current.ready).toBe(true));
    expect(result.current.enabled).toBe(false);
  });

  it("waits for the token when the check is on and forgets it on reset", async () => {
    const { result } = renderHook(() => useTurnstile(async () => ({ enabled: true, siteKey: "site" })), { wrapper });
    await waitFor(() => expect(result.current.enabled).toBe(true));
    expect(result.current.ready).toBe(false);

    act(() => result.current.widgetProps.onToken("tok"));
    expect(result.current.ready).toBe(true);
    expect(result.current.token).toBe("tok");

    act(() => result.current.reset());
    expect(result.current.ready).toBe(false);
  });

  it("renew asks the widget for a fresh token and resolves with it", async () => {
    const { result } = renderHook(() => useTurnstile(async () => ({ enabled: true, siteKey: "site" })), { wrapper });
    await waitFor(() => expect(result.current.enabled).toBe(true));
    act(() => result.current.widgetProps.onToken("first"));

    let fresh: Promise<string | null> | undefined;
    act(() => {
      fresh = result.current.renew();
    });
    expect(result.current.token).toBeNull();
    act(() => result.current.widgetProps.onToken("second"));

    await expect(fresh).resolves.toBe("second");
    expect(result.current.token).toBe("second");
  });

  it("renew resolves null at once when the check is off", async () => {
    const { result } = renderHook(() => useTurnstile(async () => ({ enabled: false, siteKey: null })), { wrapper });
    await waitFor(() => expect(result.current.ready).toBe(true));
    await expect(result.current.renew()).resolves.toBeNull();
  });

  it("does not block the form when the config route fails: the backend decides", async () => {
    const { result } = renderHook(
      () =>
        useTurnstile(async () => {
          throw new Error("offline");
        }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.ready).toBe(true));
    expect(result.current.enabled).toBe(false);
  });
});

describe("turnstileHeaders", () => {
  it("sends the token in the standard header, and nothing without one", () => {
    expect(turnstileHeaders("tok")).toEqual({ "X-Turnstile-Token": "tok" });
    expect(turnstileHeaders(null)).toEqual({});
  });
});
