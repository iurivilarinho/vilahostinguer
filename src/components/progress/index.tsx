import { cn } from "@/lib/merge-classes";

type ProgressProps = {
  value: number;
  className?: string;
  "aria-label"?: string;
};

const WARNING_AT = 75;
const DANGER_AT = 90;

/** Barra de uso (0–100). Muda de cor perto do limite, como nos painéis de hospedagem. */
export const Progress = ({ value, className, ...props }: ProgressProps) => {
  const clamped = Math.min(Math.max(value, 0), 100);
  return (
    <div
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(clamped)}
      className={cn("h-2 w-full overflow-hidden rounded-full bg-muted", className)}
      {...props}
    >
      <div
        className={cn(
          "h-full rounded-full bg-primary transition-[width] duration-500",
          clamped >= WARNING_AT && "bg-warning",
          clamped >= DANGER_AT && "bg-destructive",
        )}
        style={{ width: `${clamped}%` }}
      />
    </div>
  );
};
