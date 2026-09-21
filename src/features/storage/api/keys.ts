export const storageKeys = {
  all: ["partitions"] as const,
  device: (deviceId: number) => [...storageKeys.all, deviceId] as const,
};
