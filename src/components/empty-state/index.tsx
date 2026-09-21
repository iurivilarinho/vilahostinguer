import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";
import { Typography } from "../typography";

type EmptyStateProps = {
  icon: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
  className?: string;
};

/** Lista vazia: explica o que falta e oferece o próximo passo útil. */
export const EmptyState = ({ icon, title, description, action, className }: EmptyStateProps) => (
  <div className={cn("flex flex-col items-center gap-3 px-6 py-12 text-center", className)}>
    <div className="flex size-12 items-center justify-center rounded-full bg-primary-soft text-primary [&_svg]:size-6">{icon}</div>
    <Typography variant="title-sm">{title}</Typography>
    <Typography variant="body-sm" className="max-w-md text-muted-foreground">
      {description}
    </Typography>
    {action && <div className="pt-2">{action}</div>}
  </div>
);
