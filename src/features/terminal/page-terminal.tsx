import { Plus, SquareTerminal, X } from "lucide-react";
import { useState } from "react";
import { Button, Card, EmptyState, PageHeader, Select } from "@/components";
import { useDevicesQuery } from "@/features/devices/api";
import { cn } from "@/lib/merge-classes";
import { TerminalView } from "./components/terminal-view";
import { useTerminalSessionsStore } from "./model";

const READY_DEVICES_PARAMS = { page: 0, size: 100, filter: { active: true, status: ["READY" as const] } };

export const PageTerminal = () => {
  const sessions = useTerminalSessionsStore((state) => state.sessions);
  const activeKey = useTerminalSessionsStore((state) => state.activeKey);
  const openSession = useTerminalSessionsStore((state) => state.openSession);
  const closeSession = useTerminalSessionsStore((state) => state.closeSession);
  const activate = useTerminalSessionsStore((state) => state.activate);
  const { data: devices } = useDevicesQuery(READY_DEVICES_PARAMS);
  const [selectedId, setSelectedId] = useState("");

  const readyDevices = devices?.data ?? [];

  const handleOpen = () => {
    const device = readyDevices.find((candidate) => String(candidate.id) === selectedId);
    if (device) {
      openSession(device.id, device.name);
    }
  };

  return (
    <>
      <PageHeader
        title="Terminal"
        description="Linha de comando direto nos dispositivos, com várias sessões em abas."
        actions={
          <>
            <Select value={selectedId} onChange={(event) => setSelectedId(event.target.value)} aria-label="Dispositivo" className="w-64">
              <option value="">Escolha um dispositivo</option>
              {readyDevices.map((device) => (
                <option key={device.id} value={String(device.id)} disabled={!device.online}>
                  {device.name}
                  {device.online ? "" : " (desconectado)"}
                </option>
              ))}
            </Select>
            <Button onClick={handleOpen} disabled={!selectedId}>
              <Plus />
              Abrir sessão
            </Button>
          </>
        }
      />

      {sessions.length === 0 ? (
        <Card>
          <EmptyState
            icon={<SquareTerminal />}
            title="Nenhuma sessão aberta"
            description="Escolha um dispositivo pronto e abra uma sessão. Cada aba é um shell independente; fechar a aba encerra o processo no dispositivo."
          />
        </Card>
      ) : (
        <Card className="flex h-[calc(100vh-15rem)] min-h-96 flex-col overflow-hidden">
          <div role="tablist" className="flex shrink-0 gap-1 overflow-x-auto border-b border-border bg-muted/40 px-2 pt-2">
            {sessions.map((session) => (
              <div
                key={session.key}
                className={cn(
                  "flex items-center gap-1 rounded-t-lg border border-b-0 border-transparent px-3 py-1.5",
                  session.key === activeKey && "border-border bg-card",
                )}
              >
                <button type="button" role="tab" aria-selected={session.key === activeKey} className="nav-link cursor-pointer" onClick={() => activate(session.key)}>
                  {session.deviceName}
                </button>
                <button
                  type="button"
                  aria-label={`Fechar sessão de ${session.deviceName}`}
                  className="cursor-pointer rounded p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                  onClick={() => closeSession(session.key)}
                >
                  <X className="size-3.5" />
                </button>
              </div>
            ))}
          </div>
          <div className="flex min-h-0 flex-1 p-3">
            {sessions.map((session) => (
              <TerminalView key={session.key} deviceId={session.deviceId} visible={session.key === activeKey} className="flex-1" />
            ))}
          </div>
        </Card>
      )}
    </>
  );
};
