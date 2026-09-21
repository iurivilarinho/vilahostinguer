import { useState } from "react";
import { PageHeader, Select } from "@/components";
import type { OperationFilter, OperationStatus, OperationType } from "../api";
import { OperationsTable } from "../components/operations-table";

const STATUS_OPTIONS: { value: OperationStatus; label: string }[] = [
  { value: "RUNNING", label: "Em execução" },
  { value: "SUCCEEDED", label: "Concluídas" },
  { value: "FAILED", label: "Com falha" },
  { value: "CANCELED", label: "Canceladas" },
];

const TYPE_OPTIONS: { value: OperationType; label: string }[] = [
  { value: "INSTALL_APP", label: "Instalações" },
  { value: "REMOVE_APP", label: "Remoções" },
  { value: "SERVICE_ACTION", label: "Serviços" },
  { value: "SYSTEM_UPGRADE", label: "Atualizações do sistema" },
  { value: "BACKUP", label: "Backups" },
  { value: "RESTORE", label: "Restaurações" },
  { value: "FORMAT_PARTITION", label: "Formatações" },
  { value: "MACHINE_CREATE", label: "Criação de máquinas" },
  { value: "MACHINE_ACTION", label: "Ligar/desligar máquinas" },
  { value: "MACHINE_REMOVE", label: "Remoção de máquinas" },
  { value: "MACHINE_BACKUP", label: "Backups de máquinas" },
  { value: "MACHINE_RESTORE", label: "Restauração de máquinas" },
  { value: "MACHINE_REINSTALL", label: "Reinstalação de máquinas" },
  { value: "VOLUME_ATTACH", label: "Conexão de discos do PC" },
  { value: "VOLUME_DETACH", label: "Desconexão de discos do PC" },
];

export const PageOperations = () => {
  const [status, setStatus] = useState<OperationStatus | "">("");
  const [type, setType] = useState<OperationType | "">("");

  const filter: OperationFilter = {
    status: status ? [status] : undefined,
    type: type ? [type] : undefined,
  };

  return (
    <>
      <PageHeader
        title="Atividades"
        description="Tudo o que o painel executou nos dispositivos, com a saída completa."
        actions={
          <>
            <Select value={type} onChange={(event) => setType(event.target.value as OperationType | "")} aria-label="Tipo" className="w-52">
              <option value="">Todos os tipos</option>
              {TYPE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
            <Select value={status} onChange={(event) => setStatus(event.target.value as OperationStatus | "")} aria-label="Situação" className="w-44">
              <option value="">Todas as situações</option>
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          </>
        }
      />
      <OperationsTable filter={filter} storageKey="operationsPagination" />
    </>
  );
};
