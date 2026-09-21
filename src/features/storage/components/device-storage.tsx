import { Eraser, HardDrive, RefreshCw, ShieldCheck } from "lucide-react";
import { useState } from "react";
import {
  Badge,
  Button,
  Card,
  CardHeader,
  EmptyState,
  Progress,
  QueryErrorState,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tooltip,
  Typography,
} from "@/components";
import { getApiErrorMessage } from "@/app/utils/get-api-error-message";
import { formatBytes, percent } from "@/lib/format";
import { usePartitionsQuery, type PartitionDto } from "../api";
import { FormatPartitionDialog } from "./format-partition-dialog";

type DeviceStorageProps = {
  deviceId: number;
};

export const DeviceStorage = ({ deviceId }: DeviceStorageProps) => {
  const { data: partitions, isLoading, isError, error, refetch, isFetching } = usePartitionsQuery(deviceId);
  const [formatting, setFormatting] = useState<PartitionDto | null>(null);

  return (
    <Card>
      <CardHeader className="flex-nowrap">
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <Typography variant="title-md">Discos e partições</Typography>
          <Typography variant="body-sm" className="text-muted-foreground">
            Partições de sistema do aparelho e as montadas ficam protegidas. Só as de dados (como userdata) ou de cartões e pendrives podem ser formatadas.
          </Typography>
        </div>
        <Button variant="outline" onClick={() => refetch()} loading={isFetching}>
          <RefreshCw />
          Reler
        </Button>
      </CardHeader>

      {isError ? (
        <QueryErrorState message={getApiErrorMessage(error, "Não foi possível ler as partições.")} onRetry={() => refetch()} retrying={isFetching} className="m-5" />
      ) : isLoading ? (
        <div className="flex flex-col gap-3 p-5">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
        </div>
      ) : !partitions || partitions.length === 0 ? (
        <EmptyState icon={<HardDrive />} title="Nenhuma partição encontrada" description="O sistema não expôs /proc/partitions." />
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Partição</TableHead>
              <TableHead>Sistema de arquivos</TableHead>
              <TableHead>Tamanho</TableHead>
              <TableHead className="w-56">Uso</TableHead>
              <TableHead>Situação</TableHead>
              <TableHead className="w-28" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {partitions.map((partition) => (
              <TableRow key={partition.name} className={partition.disk ? "bg-muted/30" : undefined}>
                <TableCell>
                  <div className="flex flex-col">
                    <Typography variant="mono">{partition.name}</Typography>
                    <Typography variant="caption">
                      {[partition.disk ? "disco" : partition.partitionName, partition.label].filter(Boolean).join(" · ") || "—"}
                    </Typography>
                  </div>
                </TableCell>
                <TableCell>
                  <Typography variant="body-sm">{partition.fileSystem ?? "—"}</Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body-sm">{formatBytes(partition.sizeBytes)}</Typography>
                </TableCell>
                <TableCell>
                  {partition.mountPoint && partition.usedBytes !== null ? (
                    <div className="flex flex-col gap-1">
                      <Typography variant="caption">
                        {partition.mountPoint} · {formatBytes(partition.usedBytes)}
                      </Typography>
                      <Progress value={percent(partition.usedBytes, partition.sizeBytes)} aria-label={`Uso de ${partition.name}`} />
                    </div>
                  ) : (
                    <Typography variant="caption">{partition.mountPoint ?? "Não montada"}</Typography>
                  )}
                </TableCell>
                <TableCell>
                  {partition.formattable ? (
                    <Badge tone="warning">Pode formatar</Badge>
                  ) : (
                    <Tooltip content={partition.protectionReason}>
                      <Badge tone="success">
                        <ShieldCheck className="size-3" />
                        Protegida
                      </Badge>
                    </Tooltip>
                  )}
                </TableCell>
                <TableCell className="text-right">
                  {partition.formattable && (
                    <Button variant="outline" size="sm" onClick={() => setFormatting(partition)}>
                      <Eraser />
                      Formatar
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <FormatPartitionDialog deviceId={deviceId} partition={formatting} onClose={() => setFormatting(null)} />
    </Card>
  );
};
