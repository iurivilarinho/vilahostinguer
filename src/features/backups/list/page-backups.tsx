import { Plus, Search } from "lucide-react";
import { useState } from "react";
import { Button, Input, PageHeader, Tabs } from "@/components";
import type { BackupFilter } from "../api";
import { BackupsTable } from "../components/backups-table";
import { BackupSheet } from "../form/backup-sheet";

type ListView = "available" | "all";

export const PageBackups = () => {
  const [view, setView] = useState<ListView>("available");
  const [search, setSearch] = useState("");
  const [sheetOpen, setSheetOpen] = useState(false);

  const filter: BackupFilter = {
    search: search.trim() || undefined,
    status: view === "available" ? ["AVAILABLE", "CREATING"] : undefined,
  };

  return (
    <>
      <PageHeader
        title="Backups"
        description="Cópias das pastas dos dispositivos, guardadas neste computador."
        actions={
          <Button onClick={() => setSheetOpen(true)}>
            <Plus />
            Novo backup
          </Button>
        }
      />
      <div className="flex flex-wrap items-end justify-between gap-4">
        <Tabs
          value={view}
          onValueChange={setView}
          items={[
            { value: "available", label: "Disponíveis" },
            { value: "all", label: "Histórico completo" },
          ]}
        />
        <div className="relative w-72">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar pelo nome" className="pl-9" />
        </div>
      </div>
      <BackupsTable
        filter={filter}
        storageKey="backupsPagination"
        emptyAction={
          <Button onClick={() => setSheetOpen(true)}>
            <Plus />
            Novo backup
          </Button>
        }
      />
      <BackupSheet open={sheetOpen} onOpenChange={setSheetOpen} />
    </>
  );
};
