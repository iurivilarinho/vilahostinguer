export const serverKeys = {
  all: ["portal-servers"] as const,
  list: () => [...serverKeys.all, "list"] as const,
  detail: (id: number) => [...serverKeys.all, "detail", id] as const,
  stats: (id: number) => [...serverKeys.all, "stats", id] as const,
  distributions: (id: number) => [...serverKeys.all, "distributions", id] as const,
  backups: (id: number) => [...serverKeys.all, "backups", id] as const,
  operation: (id: number, operationId: number) => [...serverKeys.all, "operation", id, operationId] as const,
};
