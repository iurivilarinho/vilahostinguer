import { OperationsTable } from "@/features/operations";
import { useDeviceOutlet } from "../device-outlet";

export const SectionActivity = () => {
  const { device } = useDeviceOutlet();
  return <OperationsTable filter={{ deviceId: device.id }} showDevice={false} storageKey="deviceOperationsPagination" />;
};
