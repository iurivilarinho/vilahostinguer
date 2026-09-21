import type { ComponentProps } from "react";
import { cn } from "@/lib/merge-classes";

export const inputClassName =
  "h-10 w-full min-w-0 rounded-lg border border-input bg-card px-3 text-sm text-foreground placeholder:text-muted-foreground transition-colors focus:border-primary focus:outline-none disabled:cursor-not-allowed disabled:opacity-60 aria-invalid:border-destructive";

type InputProps = ComponentProps<"input">;

export const Input = ({ className, ...props }: InputProps) => <input className={cn(inputClassName, className)} {...props} />;
