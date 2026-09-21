import { Archive, ArchiveRestore, Globe, MoreHorizontal, Pencil, RefreshCw } from "lucide-react";
import { Badge, Button, Card, DropdownMenu, Typography } from "@/components";
import { formatRelative } from "@/lib/format";
import { cn } from "@/lib/merge-classes";
import type { DdnsSyncResult, DomainDto } from "../api";

const RESULT_TONE: Record<DdnsSyncResult, "neutral" | "success" | "destructive"> = {
  PENDING: "neutral",
  SYNCED: "success",
  FAILED: "destructive",
};

type DomainCardProps = {
  domain: DomainDto;
  publicIp: string | null;
  syncing: boolean;
  onSync: (domain: DomainDto) => void;
  onEdit: (domain: DomainDto) => void;
  onChangeActive: (domain: DomainDto, active: boolean) => void;
};

export const DomainCard = ({ domain, publicIp, syncing, onSync, onEdit, onChangeActive }: DomainCardProps) => {
  const pointsHere = publicIp !== null && domain.resolvedIp === publicIp;

  return (
    <Card className="flex flex-col gap-4 p-5">
      <div className="flex items-start gap-3">
        <div className={cn("flex size-11 shrink-0 items-center justify-center rounded-lg", domain.lastResult === "SYNCED" ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground")}>
          <Globe className="size-5" />
        </div>
        <div className="flex min-w-0 flex-1 flex-col">
          <Typography variant="title-sm" className="truncate" title={domain.name}>
            {domain.name}
          </Typography>
          <Typography variant="caption">
            {domain.providerDescription}
            {domain.wildcard ? " · com curinga" : ""}
          </Typography>
        </div>
        <Badge tone={RESULT_TONE[domain.lastResult]}>{domain.lastResultDescription}</Badge>
      </div>

      <dl className="grid grid-cols-2 gap-3">
        <div className="flex flex-col gap-0.5">
          <Typography variant="caption" as="span">
            Aponta para
          </Typography>
          <Typography variant="mono" className={cn(domain.resolvedIp && !pointsHere && "text-warning-foreground")}>
            {domain.resolvedIp ?? "não resolve"}
          </Typography>
        </div>
        <div className="flex flex-col gap-0.5">
          <Typography variant="caption" as="span">
            Última atualização
          </Typography>
          <Typography variant="body-sm">{domain.lastSyncAt ? formatRelative(domain.lastSyncAt) : "nunca"}</Typography>
        </div>
      </dl>

      {domain.lastMessage && (
        <Typography variant="caption" as="p" className={cn("rounded-md bg-muted px-3 py-2", domain.lastResult === "FAILED" && "bg-destructive-soft text-destructive-foreground")}>
          {domain.lastMessage}
        </Typography>
      )}

      <div className="mt-auto flex items-center justify-between gap-2">
        <Typography variant="caption">
          {domain.ddnsEnabled ? `Automático · até ${domain.intervalMinutes} min` : "Só manual"}
        </Typography>
        <div className="flex items-center gap-2">
          <Button size="sm" variant="outline" onClick={() => onSync(domain)} loading={syncing} disabled={!domain.active}>
            <RefreshCw />
            {domain.provider === "MANUAL" ? "Conferir" : "Atualizar agora"}
          </Button>
          <DropdownMenu
            trigger={
              <Button variant="ghost" size="icon" aria-label={`Ações de ${domain.name}`}>
                <MoreHorizontal />
              </Button>
            }
            items={[
              { label: "Editar", icon: <Pencil />, onSelect: () => onEdit(domain) },
              domain.active
                ? { label: "Arquivar", icon: <Archive />, destructive: true, onSelect: () => onChangeActive(domain, false) }
                : { label: "Reativar", icon: <ArchiveRestore />, onSelect: () => onChangeActive(domain, true) },
            ]}
          />
        </div>
      </div>
    </Card>
  );
};
