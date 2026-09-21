import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";
import { Typography } from "../typography";

type PageHeaderProps = {
  title: string;
  description?: string;
  actions?: ReactNode;
  className?: string;
};

export const PageHeader = ({ title, description, actions, className }: PageHeaderProps) => (
  <header className={cn("flex flex-wrap items-end justify-between gap-4", className)}>
    <div className="flex min-w-0 flex-col gap-1">
      <Typography variant="display-sm">{title}</Typography>
      {description && (
        <Typography variant="body-sm" className="text-muted-foreground">
          {description}
        </Typography>
      )}
    </div>
    {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
  </header>
);
