import { create } from "zustand";

export type Theme = "light" | "dark";

const STORAGE_KEY = "bancada.theme";

const readTheme = (): Theme => {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === "light" || stored === "dark") {
    return stored;
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
};

const applyTheme = (theme: Theme) => {
  document.documentElement.classList.toggle("dark", theme === "dark");
};

type ThemeState = {
  theme: Theme;
  toggle: () => void;
};

export const useThemeStore = create<ThemeState>((set, get) => {
  const initial = readTheme();
  applyTheme(initial);
  return {
    theme: initial,
    toggle: () => {
      const next: Theme = get().theme === "dark" ? "light" : "dark";
      localStorage.setItem(STORAGE_KEY, next);
      applyTheme(next);
      set({ theme: next });
    },
  };
});
