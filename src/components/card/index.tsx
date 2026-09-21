import type { ComponentProps } from "react";
import { cn } from "@/lib/merge-classes";

type CardProps = ComponentProps<"section">;
type CardHeaderProps = ComponentProps<"header">;
type CardContentProps = ComponentProps<"div">;
type CardFooterProps = ComponentProps<"footer">;

export const Card = ({ className, ...props }: CardProps) => (
  <section className={cn("rounded-lg border border-border bg-card text-card-foreground shadow-card", className)} {...props} />
);

export const CardHeader = ({ className, ...props }: CardHeaderProps) => (
  <header className={cn("flex flex-wrap items-start justify-between gap-3 border-b border-border px-5 py-4", className)} {...props} />
);

export const CardContent = ({ className, ...props }: CardContentProps) => <div className={cn("p-5", className)} {...props} />;

export const CardFooter = ({ className, ...props }: CardFooterProps) => (
  <footer className={cn("flex items-center justify-end gap-3 border-t border-border px-5 py-3", className)} {...props} />
);
