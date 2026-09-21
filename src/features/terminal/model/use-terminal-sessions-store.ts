import { create } from "zustand";

export type TerminalSession = {
  key: string;
  deviceId: number;
  deviceName: string;
};

type TerminalSessionsState = {
  sessions: TerminalSession[];
  activeKey: string | null;
  openSession: (deviceId: number, deviceName: string) => void;
  closeSession: (key: string) => void;
  activate: (key: string) => void;
};

/** Abas abertas na tela de Terminal. Cada aba é uma sessão SSH independente. */
export const useTerminalSessionsStore = create<TerminalSessionsState>((set, get) => ({
  sessions: [],
  activeKey: null,
  openSession: (deviceId, deviceName) => {
    const key = `${deviceId}-${Date.now()}`;
    set({ sessions: [...get().sessions, { key, deviceId, deviceName }], activeKey: key });
  },
  closeSession: (key) => {
    const sessions = get().sessions.filter((session) => session.key !== key);
    const activeKey = get().activeKey === key ? (sessions.at(-1)?.key ?? null) : get().activeKey;
    set({ sessions, activeKey });
  },
  activate: (key) => set({ activeKey: key }),
}));
