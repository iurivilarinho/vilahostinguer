import { useOutletContext } from "react-router-dom";
import type { DeviceDto } from "../api";

export type DeviceOutletContext = {
  device: DeviceDto;
};

/** As seções do dispositivo recebem o dispositivo já carregado pela página que as envolve. */
export const useDeviceOutlet = () => useOutletContext<DeviceOutletContext>();
