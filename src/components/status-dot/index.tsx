import { cn } from "@/lib/merge-classes";

type StatusDotProps = {
  online: boolean;
  className?: string;
};

export const StatusDot = ({ online, className }: StatusDotProps) => (
  <span className={cn("relative inline-flex size-2.5 shrink-0", className)} aria-label={online ? "Online" : "Desconectado"} role="img">
    {online && <span className="absolute inline-flex size-full animate-ping rounded-full bg-success opacity-50" />}
    <span className={cn("relative inline-flex size-2.5 rounded-full", online ? "bg-success" : "bg-muted-foreground/50")} />
  </span>
);
