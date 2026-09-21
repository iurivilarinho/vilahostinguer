export const appKeys = {
  all: ["apps"] as const,
  device: (deviceId: number) => [...appKeys.all, deviceId] as const,
};
