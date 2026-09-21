import type { ComponentProps } from "react";
import { cn } from "@/lib/merge-classes";

type TextareaProps = ComponentProps<"textarea">;

export const Textarea = ({ className, ...props }: TextareaProps) => (
  <textarea
    className={cn(
      "min-h-24 w-full rounded-lg border border-input bg-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none disabled:opacity-60 aria-invalid:border-destructive",
      className,
    )}
    {...props}
  />
);
