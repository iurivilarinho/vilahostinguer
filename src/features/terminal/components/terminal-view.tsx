import { FitAddon } from "@xterm/addon-fit";
import { Terminal } from "@xterm/xterm";
import { RotateCw } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Button, Typography } from "@/components";
import { tokenStorage } from "@/app/utils/token-storage";
import { cn } from "@/lib/merge-classes";

type TerminalViewProps = {
  /** Shell do dispositivo. */
  deviceId?: number;
  /** Shell dentro de uma máquina (docker exec). Tem prioridade sobre o dispositivo. */
  machineId?: number;
  visible?: boolean;
  className?: string;
};

type ConnectionState = "connecting" | "open" | "closed";

const readToken = (name: string): string => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

const socketUrl = (target: string, columns: number, rows: number): string => {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return tokenStorage.withToken(`${protocol}://${window.location.host}/ws/terminal?${target}&cols=${columns}&rows=${rows}`);
};

/**
 * Terminal de verdade (PTY via SSH) desenhado pelo xterm.js. As teclas vão como JSON e a saída
 * chega em bytes crus, que o próprio xterm decodifica.
 */
export const TerminalView = ({ deviceId, machineId, visible = true, className }: TerminalViewProps) => {
  const target = machineId !== undefined ? `machineId=${machineId}` : `deviceId=${deviceId ?? 0}`;
  const containerRef = useRef<HTMLDivElement>(null);
  const fitRef = useRef<FitAddon | null>(null);
  const [state, setState] = useState<ConnectionState>("connecting");
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }
    const terminal = new Terminal({
      cursorBlink: true,
      fontFamily: '"Cascadia Mono", Consolas, ui-monospace, monospace',
      fontSize: 13,
      scrollback: 5000,
      theme: {
        background: readToken("--terminal") || "#1d1e20",
        foreground: readToken("--terminal-foreground") || "#e8e8ee",
        cursor: readToken("--primary") || "#673de6",
        selectionBackground: "#673de655",
      },
    });
    const fit = new FitAddon();
    fitRef.current = fit;
    terminal.loadAddon(fit);
    terminal.open(container);
    fit.fit();

    setState("connecting");
    const socket = new WebSocket(socketUrl(target, terminal.cols, terminal.rows));
    socket.binaryType = "arraybuffer";
    socket.onopen = () => {
      setState("open");
      terminal.focus();
    };
    socket.onmessage = (event: MessageEvent<ArrayBuffer | string>) => {
      terminal.write(typeof event.data === "string" ? event.data : new Uint8Array(event.data));
    };
    socket.onclose = () => {
      setState("closed");
      terminal.write("\r\n\x1b[90m[Sessão encerrada]\x1b[0m\r\n");
    };

    const input = terminal.onData((data) => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: "input", data }));
      }
    });
    const resize = terminal.onResize(({ cols, rows }) => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: "resize", cols, rows }));
      }
    });
    const observer = new ResizeObserver(() => {
      if (container.offsetParent !== null) {
        fit.fit();
      }
    });
    observer.observe(container);

    return () => {
      observer.disconnect();
      input.dispose();
      resize.dispose();
      socket.close();
      terminal.dispose();
      fitRef.current = null;
    };
  }, [target, attempt]);

  useEffect(() => {
    if (visible) {
      fitRef.current?.fit();
    }
  }, [visible]);

  return (
    <div className={cn("relative flex min-h-0 flex-col overflow-hidden rounded-lg bg-terminal", !visible && "hidden", className)}>
      <div ref={containerRef} className="min-h-0 flex-1 p-3" />
      {state === "closed" && (
        <div className="flex items-center justify-between gap-3 border-t border-white/10 px-4 py-2">
          <Typography variant="caption" className="text-terminal-foreground/70">
            A conexão foi encerrada.
          </Typography>
          <Button size="sm" variant="secondary" onClick={() => setAttempt((current) => current + 1)}>
            <RotateCw />
            Reconectar
          </Button>
        </div>
      )}
    </div>
  );
};
