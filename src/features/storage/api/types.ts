export type FileSystemType = "EXT4" | "VFAT";

export type PartitionDto = {
  name: string;
  sizeBytes: number;
  disk: boolean;
  fileSystem: string | null;
  label: string | null;
  partitionName: string | null;
  mountPoint: string | null;
  usedBytes: number | null;
  formattable: boolean;
  protectionReason: string | null;
};

export type FormatPartitionRequest = {
  deviceId: number;
  partition: string;
  fileSystem: FileSystemType;
  label: string;
  confirmation: string;
};
