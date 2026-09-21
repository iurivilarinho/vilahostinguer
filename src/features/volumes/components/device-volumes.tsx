import { Database } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, CardHeader, EmptyState, QueryErrorState, Skeleton, Typography } from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { Rotas } from "@/app/variables/rotas";
import { useVolumesQuery } from "../api";
import { VolumesTable } from "./volumes-table";

type DeviceVolumesProps = {
  deviceId: number;
};

/** Discos do PC entregues a este dispositivo (ou às máquinas dele). */
export const DeviceVolumes = ({ deviceId }: DeviceVolumesProps) => {
  const { data, isLoading, isError, error, refetch, isFetching } = useVolumesQuery({ page: 0, size: 100, filter: { deviceId } });
  const volumes = data?.data ?? [];

  return (
    <Card>
      <CardHeader className="flex-nowrap">
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <Typography variant="title-md">Discos do PC</Typography>
          <Typography variant="body-sm" className="text-muted-foreground">
            Espaço dos SSDs e HDs do computador usado por este dispositivo pela rede.
          </Typography>
        </div>
        <Link to={Rotas.volumes} className="inline-link text-sm">
          Gerenciar discos
        </Link>
      </CardHeader>
      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível carregar os discos.")} onRetry={() => refetch()} retrying={isFetching} className="m-5" />
      ) : isLoading ? (
        <Skeleton className="m-5 h-10" />
      ) : volumes.length === 0 ? (
        <EmptyState
          icon={<Database />}
          title="Nenhum disco do PC aqui"
          description="Crie um disco em Discos do PC e conecte a este dispositivo ou a uma das máquinas dele."
        />
      ) : (
        <VolumesTable volumes={volumes} presetDeviceId={deviceId} />
      )}
    </Card>
  );
};
