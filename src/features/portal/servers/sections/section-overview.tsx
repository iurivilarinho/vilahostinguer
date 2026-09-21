import { Archive, Copy, Cpu, Globe, HardDrive, MemoryStick, Terminal } from "lucide-react";
import type { ReactNode } from "react";
import { Button, Card, CardContent, CardHeader, Progress, Typography, notify } from "@/components";
import { formatDate } from "@/lib/format";
import { useServerStatsQuery, type ServerDto } from "../api";
import { serverState } from "../components/server-status-badge";

const InfoRow = ({ label, children }: { label: string; children: ReactNode }) => (
  <div className="flex flex-col gap-0.5">
    <Typography variant="caption">{label}</Typography>
    <Typography variant="body-sm" as="div" className="font-medium">
      {children}
    </Typography>
  </div>
);

const copy = async (text: string, label: string) => {
  await navigator.clipboard.writeText(text);
  notify.success(`${label} copiado`);
};

/** Acesso, recursos do plano e uso ao vivo. */
export const SectionOverview = ({ server }: { server: ServerDto }) => {
  const running = serverState(server).running;
  const { data: stats } = useServerStatsQuery(server.id, running);

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <Typography variant="title-sm">Acesso SSH</Typography>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {server.sshCommand ? (
            <div className="flex items-center gap-2 rounded-md bg-muted px-3 py-2">
              <Terminal className="size-4 shrink-0 text-muted-foreground" />
              <Typography variant="mono" className="min-w-0 flex-1 truncate">
                {server.sshCommand}
              </Typography>
              <Button variant="ghost" size="icon" aria-label="Copiar comando" onClick={() => copy(server.sshCommand ?? "", "Comando")}>
                <Copy />
              </Button>
            </div>
          ) : (
            <Typography variant="body-sm" className="text-muted-foreground">
              O acesso aparece aqui quando o servidor estiver pronto.
            </Typography>
          )}
          <div className="grid grid-cols-3 gap-4">
            <InfoRow label="Endereço">{server.sshHost ?? "—"}</InfoRow>
            <InfoRow label="Porta">{server.sshPort ?? "—"}</InfoRow>
            <InfoRow label="Usuário">{server.username}</InfoRow>
          </div>
          {server.siteHostname && (
            <div className="flex items-center gap-2 border-t border-border pt-4">
              <Globe className="size-4 text-primary" />
              <a href={`http://${server.siteHostname}`} target="_blank" rel="noreferrer" className="inline-link text-sm">
                {server.siteHostname}
              </a>
              <Typography variant="caption">(sites nas portas 80 e 443 do servidor)</Typography>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <Typography variant="title-sm">Uso agora</Typography>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {running ? (
            <>
              <div className="flex flex-col gap-2">
                <div className="flex justify-between">
                  <Typography variant="caption">CPU</Typography>
                  <Typography variant="caption" className="text-foreground">
                    {stats?.cpuPercent?.toFixed(1) ?? "—"}%
                  </Typography>
                </div>
                <Progress value={Math.min(100, stats?.cpuPercent ?? 0)} aria-label="CPU" />
              </div>
              <div className="flex flex-col gap-2">
                <div className="flex justify-between">
                  <Typography variant="caption">Memória</Typography>
                  <Typography variant="caption" className="text-foreground">
                    {stats?.memoryUsage ?? "—"}
                  </Typography>
                </div>
                <Progress value={stats?.memoryPercent ?? 0} aria-label="Memória" />
              </div>
            </>
          ) : (
            <Typography variant="body-sm" className="text-muted-foreground">
              O uso aparece com o servidor ligado.
            </Typography>
          )}
        </CardContent>
      </Card>

      <Card className="lg:col-span-2">
        <CardHeader>
          <Typography variant="title-sm">Plano {server.planName}</Typography>
          <Typography variant="caption">
            {server.cancelAtPeriodEnd ? "Encerra em " : "Renova em "}
            {formatDate(server.nextDueDate)}
          </Typography>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-4">
          <InfoRow label="CPU">
            <span className="flex items-center gap-2">
              <Cpu className="size-4 text-primary" />
              {server.cpuLimit} núcleo(s)
            </span>
          </InfoRow>
          <InfoRow label="Memória">
            <span className="flex items-center gap-2">
              <MemoryStick className="size-4 text-primary" />
              {server.memoryMb} MB
            </span>
          </InfoRow>
          <InfoRow label="Disco">
            <span className="flex items-center gap-2">
              <HardDrive className="size-4 text-primary" />
              {server.diskGb} GB
            </span>
          </InfoRow>
          <InfoRow label="Backups">
            <span className="flex items-center gap-2">
              <Archive className="size-4 text-primary" />
              até {server.backupSlots}
            </span>
          </InfoRow>
        </CardContent>
      </Card>
    </div>
  );
};
