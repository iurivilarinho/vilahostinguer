export const fileKeys = {
  all: ["files"] as const,
  device: (deviceId: number) => [...fileKeys.all, deviceId] as const,
  directory: (deviceId: number, path: string) => [...fileKeys.device(deviceId), path] as const,
};
