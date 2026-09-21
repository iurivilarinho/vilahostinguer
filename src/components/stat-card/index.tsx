import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";
import { Typography } from "../typography";

type StatCardProps = {
  label: string;
  value: ReactNode;
  icon: ReactNode;
  hint?: string;
  tone?: "primary" | "success" | "warning" | "destructive" | "info";
  className?: string;
};

const toneClass: Record<NonNullable<StatCardProps["tone"]>, string> = {
  primary: "bg-primary-soft text-primary",
  success: "bg-success-soft text-success-foreground",
  warning: "bg-warning-soft text-warning-foreground",
  destructive: "bg-destructive-soft text-destructive-foreground",
  info: "bg-info-soft text-info-foreground",
};

export const StatCard = ({ label, value, icon, hint, tone = "primary", className }: StatCardProps) => (
  <div className={cn("flex items-center gap-4 rounded-lg border border-border bg-card p-5 shadow-card", className)}>
    <div className={cn("flex size-11 shrink-0 items-center justify-center rounded-lg [&_svg]:size-5", toneClass[tone])}>{icon}</div>
    <div className="flex min-w-0 flex-col">
      <Typography variant="caption">{label}</Typography>
      <Typography variant="title-lg" as="span">
        {value}
      </Typography>
      {hint && (
        <Typography variant="caption" className="truncate">
          {hint}
        </Typography>
      )}
    </div>
  </div>
);
