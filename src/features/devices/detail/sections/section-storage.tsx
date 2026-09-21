import { DeviceStorage } from "@/features/storage";
import { RequiresAccess } from "../../components/requires-access";
import { useDeviceOutlet } from "../device-outlet";

export const SectionStorage = () => {
  const { device } = useDeviceOutlet();
  return (
    <RequiresAccess device={device}>
      <DeviceStorage deviceId={device.id} />
    </RequiresAccess>
  );
};
