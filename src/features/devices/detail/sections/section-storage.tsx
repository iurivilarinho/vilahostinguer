import { DeviceStorage } from "@/features/storage";
import { DeviceVolumes } from "@/features/volumes";
import { RequiresAccess } from "../../components/requires-access";
import { useDeviceOutlet } from "../device-outlet";

export const SectionStorage = () => {
  const { device } = useDeviceOutlet();
  return (
    <RequiresAccess device={device}>
      <div className="flex flex-col gap-6">
        <DeviceStorage deviceId={device.id} />
        <DeviceVolumes deviceId={device.id} />
      </div>
    </RequiresAccess>
  );
};
