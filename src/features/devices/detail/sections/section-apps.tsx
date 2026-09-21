import { DeviceApps } from "@/features/apps";
import { RequiresAccess } from "../../components/requires-access";
import { useDeviceOutlet } from "../device-outlet";

export const SectionApps = () => {
  const { device } = useDeviceOutlet();
  return (
    <RequiresAccess device={device}>
      <DeviceApps deviceId={device.id} />
    </RequiresAccess>
  );
};
