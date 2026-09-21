const BYTE_UNITS = ["B", "KB", "MB", "GB", "TB"];
const BYTE_STEP = 1024;
const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = 3600;
const SECONDS_PER_DAY = 86_400;

export const formatBytes = (bytes: number | null | undefined): string => {
  if (bytes === null || bytes === undefined || Number.isNaN(bytes)) {
    return "—";
  }
  let value = bytes;
  let unit = 0;
  while (value >= BYTE_STEP && unit < BYTE_UNITS.length - 1) {
    value /= BYTE_STEP;
    unit += 1;
  }
  const digits = unit === 0 || value >= 100 ? 0 : 1;
  return `${value.toFixed(digits).replace(".", ",")} ${BYTE_UNITS[unit]}`;
};

export const formatUptime = (seconds: number | null | undefined): string => {
  if (seconds === null || seconds === undefined) {
    return "—";
  }
  const days = Math.floor(seconds / SECONDS_PER_DAY);
  const hours = Math.floor((seconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR);
  const minutes = Math.floor((seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
  if (days > 0) {
    return `${days}d ${hours}h`;
  }
  if (hours > 0) {
    return `${hours}h ${minutes}min`;
  }
  return `${minutes}min`;
};

const dateTimeFormatter = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export const formatDateTime = (value: string | null | undefined): string => {
  if (!value) {
    return "—";
  }
  return dateTimeFormatter.format(new Date(value));
};

const relativeFormatter = new Intl.RelativeTimeFormat("pt-BR", { numeric: "auto" });

export const formatRelative = (value: string | null | undefined): string => {
  if (!value) {
    return "nunca";
  }
  const diffSeconds = Math.round((new Date(value).getTime() - Date.now()) / 1000);
  const absolute = Math.abs(diffSeconds);
  if (absolute < SECONDS_PER_MINUTE) {
    return relativeFormatter.format(diffSeconds, "second");
  }
  if (absolute < SECONDS_PER_HOUR) {
    return relativeFormatter.format(Math.round(diffSeconds / SECONDS_PER_MINUTE), "minute");
  }
  if (absolute < SECONDS_PER_DAY) {
    return relativeFormatter.format(Math.round(diffSeconds / SECONDS_PER_HOUR), "hour");
  }
  return relativeFormatter.format(Math.round(diffSeconds / SECONDS_PER_DAY), "day");
};

export const formatDuration = (start: string | null | undefined, end: string | null | undefined): string => {
  if (!start) {
    return "—";
  }
  const finish = end ? new Date(end).getTime() : Date.now();
  const seconds = Math.max(0, Math.round((finish - new Date(start).getTime()) / 1000));
  if (seconds < SECONDS_PER_MINUTE) {
    return `${seconds}s`;
  }
  return `${Math.floor(seconds / SECONDS_PER_MINUTE)}min ${seconds % SECONDS_PER_MINUTE}s`;
};

export const percent = (used: number | null | undefined, total: number | null | undefined): number => {
  if (!used || !total) {
    return 0;
  }
  return (used / total) * 100;
};
