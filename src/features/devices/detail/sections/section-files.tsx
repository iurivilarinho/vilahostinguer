import { FileManager } from "@/features/files";
import { RequiresAccess } from "../../components/requires-access";
import { useDeviceOutlet } from "../device-outlet";

export const SectionFiles = () => {
  const { device } = useDeviceOutlet();
  return (
    <RequiresAccess device={device}>
      <FileManager deviceId={device.id} initialPath={device.homeDirectory ?? "/"} />
    </RequiresAccess>
  );
};
