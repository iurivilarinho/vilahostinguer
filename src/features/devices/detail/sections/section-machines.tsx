import { DeviceMachines } from "@/features/machines";
import { RequiresAccess } from "../../components/requires-access";
import { useDeviceOutlet } from "../device-outlet";

export const SectionMachines = () => {
  const { device } = useDeviceOutlet();
  return (
    <RequiresAccess device={device}>
      <DeviceMachines device={device} />
    </RequiresAccess>
  );
};
