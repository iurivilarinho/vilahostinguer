import { Database, Plus, TriangleAlert } from "lucide-react";
import { useState } from "react";
import { Button, Card, CardHeader, EmptyState, PageHeader, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { useVolumeServerQuery, useVolumesQuery } from "../api";
import { HostDisks } from "../components/host-disks";
import { VolumesTable } from "../components/volumes-table";
import { VolumeSheet } from "../form/volume-sheet";

const ALL_VOLUMES_PARAMS = { page: 0, size: 200, sort: [{ by: "name" as const, direction: "asc" as const }] };

/** Espaço dos discos deste PC entregue aos dispositivos e às máquinas. */
export const PageVolumes = () => {
  const [sheetOpen, setSheetOpen] = useState(false);
  const server = useVolumeServerQuery();
  const { data, isLoading, isError, error, refetch, isFetching } = useVolumesQuery(ALL_VOLUMES_PARAMS);
  const volumes = data?.data ?? [];

  const newVolume = (
    <Button onClick={() => setSheetOpen(true)} disabled={!server.data}>
      <Plus />
      Novo disco
    </Button>
  );

  return (
    <>
      <PageHeader
        title="Discos do PC"
        description="Use o espaço dos SSDs e HDs deste computador nos dispositivos e nas máquinas: cada disco aparece para eles como um disco próprio."
        actions={newVolume}
      />

      {server.data && !server.data.running && (
        <div className="flex gap-3 rounded-lg border border-warning bg-warning-soft p-4 text-warning-foreground">
          <TriangleAlert className="size-5 shrink-0" />
          <Typography variant="body-sm" className="text-warning-foreground">
            {server.data.error ?? "O servidor de discos não está no ar."} Sem ele, os dispositivos não alcançam os discos.
          </Typography>
        </div>
      )}

      <section className="flex flex-col gap-3">
        <Typography variant="title-md" as="h2">
          Discos deste PC
        </Typography>
        {server.isError ? (
          <QueryErrorState message={getApiErrorMessage(server.error, "Não foi possível ler os discos do PC.")} onRetry={() => server.refetch()} retrying={server.isFetching} />
        ) : (
          <HostDisks disks={server.data?.disks} isLoading={server.isLoading} />
        )}
      </section>

      <Card>
        <CardHeader className="flex-nowrap">
          <div className="flex min-w-0 flex-1 flex-col gap-1">
            <Typography variant="title-md">Discos virtuais</Typography>
            <Typography variant="body-sm" className="text-muted-foreground">
              O dispositivo busca o disco neste PC pela rede (NBD, porta {server.data?.port ?? 10809}). Reiniciou o dispositivo? O painel conecta de novo
              sozinho. Reiniciou o PC? A pasta dá erro por alguns instantes e volta quando o painel sobe, depois de conferir o disco.
            </Typography>
          </div>
        </CardHeader>
        {isError ? (
          <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os discos.")} onRetry={() => refetch()} retrying={isFetching} className="m-5" />
        ) : isLoading ? (
          <div className="flex flex-col gap-3 p-5">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : volumes.length === 0 ? (
          <EmptyState
            icon={<Database />}
            title="Nenhum disco ainda"
            description="Crie um disco num SSD ou HD deste PC e conecte a um dispositivo ou a uma máquina."
            action={newVolume}
          />
        ) : (
          <VolumesTable volumes={volumes} />
        )}
      </Card>

      <VolumeSheet open={sheetOpen} onOpenChange={setSheetOpen} disks={server.data?.disks ?? []} />
    </>
  );
};
