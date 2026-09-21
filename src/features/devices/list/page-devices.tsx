import { Plug, Plus, RefreshCw, Search } from "lucide-react";
import { useState } from "react";
import { Button, EmptyState, Input, PageHeader, QueryErrorState, Skeleton, TableFooter, Tabs } from "@/components";
import { usePaginatedData } from "@/app/hooks/use-paginated-data";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { useDevicesQuery, useScanNowMutation, type DeviceDto, type DeviceFilter } from "../api";
import { DeviceCard } from "../components/device-card";
import { AddDeviceSheet } from "../form/add-device-sheet";

type ListView = "all" | "online" | "attention" | "archived";

const VIEW_FILTERS: Record<ListView, DeviceFilter> = {
  all: { active: true },
  online: { active: true, online: true },
  attention: { active: true, status: ["DISCOVERED", "AUTH_FAILED"] },
  archived: { active: false },
};

const SORT_BY_NAME = [{ by: "name" as const, direction: "asc" as const }];

export const PageDevices = () => {
  const [view, setView] = useState<ListView>("all");
  const [search, setSearch] = useState("");
  const [addOpen, setAddOpen] = useState(false);
  const { mutate: scanNow, isPending: isScanning } = useScanNowMutation();

  const filter: DeviceFilter = { ...VIEW_FILTERS[view], search: search.trim() || undefined };
  const {
    data: devices,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
    pagination,
    updatePagination,
  } = usePaginatedData<DeviceDto, DeviceFilter>({
    query: useDevicesQuery,
    filter,
    sort: SORT_BY_NAME,
    storageKey: "devicesPagination",
    initialSize: 25,
  });

  return (
    <>
      <PageHeader
        title="Dispositivos"
        description="Celulares, placas e mini PCs ligados por cabo USB ou pela rede."
        actions={
          <>
            <Button variant="outline" onClick={() => scanNow()} loading={isScanning}>
              <RefreshCw />
              Procurar agora
            </Button>
            <Button onClick={() => setAddOpen(true)}>
              <Plus />
              Adicionar pelo endereço
            </Button>
          </>
        }
      />

      <div className="flex flex-wrap items-end justify-between gap-4">
        <Tabs
          value={view}
          onValueChange={setView}
          items={[
            { value: "all", label: "Todos" },
            { value: "online", label: "Online" },
            { value: "attention", label: "Precisam de atenção" },
            { value: "archived", label: "Arquivados" },
          ]}
        />
        <div className="relative w-72">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por nome, IP ou sistema" className="pl-9" />
        </div>
      </div>

      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os dispositivos.")} onRetry={() => refetch()} retrying={isFetching} />
      ) : isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <Skeleton className="h-56" />
          <Skeleton className="h-56" />
          <Skeleton className="h-56" />
        </div>
      ) : devices.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border bg-card">
          <EmptyState
            icon={<Plug />}
            title={view === "archived" ? "Nenhum dispositivo arquivado" : "Nenhum dispositivo por aqui"}
            description="Conecte um aparelho pelo cabo USB com o SSH ligado: ele aparece aqui sozinho em alguns segundos. Para máquinas na rede, adicione pelo endereço."
            action={
              <Button variant="outline" onClick={() => setAddOpen(true)}>
                <Plus />
                Adicionar pelo endereço
              </Button>
            }
          />
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {devices.map((device) => (
              <DeviceCard key={device.id} device={device} />
            ))}
          </div>
          {pagination.totalPages > 1 && (
            <TableFooter
              className="rounded-lg border bg-card"
              pagination={pagination}
              onPageChange={(page) => updatePagination({ page })}
              onSizeChange={(size) => updatePagination({ size })}
            />
          )}
        </div>
      )}

      <AddDeviceSheet open={addOpen} onOpenChange={setAddOpen} />
    </>
  );
};
