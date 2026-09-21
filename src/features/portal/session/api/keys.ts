export const sessionKeys = {
  all: ["portal-session"] as const,
  me: () => [...sessionKeys.all, "me"] as const,
};
