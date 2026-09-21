export const catalogKeys = {
  all: ["portal-catalog"] as const,
  info: () => [...catalogKeys.all, "info"] as const,
  plans: () => [...catalogKeys.all, "plans"] as const,
};
