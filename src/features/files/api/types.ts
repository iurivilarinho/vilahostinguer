export type FileEntryDto = {
  name: string;
  path: string;
  directory: boolean;
  symlink: boolean;
  sizeBytes: number;
  permissions: string;
  modifiedAt: string;
};

export type FileUploadDto = {
  path: string;
  sizeBytes: number;
};

export type FilePathRequest = {
  deviceId: number;
  path: string;
};

export type FileUploadRequest = FilePathRequest & { file: File };
