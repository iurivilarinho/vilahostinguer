import { cva, type VariantProps } from "class-variance-authority";
import type { ComponentProps } from "react";
import { cn } from "@/lib/merge-classes";

export const badgeVariants = cva("inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap", {
  variants: {
    tone: {
      neutral: "bg-muted text-muted-foreground",
      primary: "bg-primary-soft text-primary-soft-foreground",
      success: "bg-success-soft text-success-foreground",
      warning: "bg-warning-soft text-warning-foreground",
      destructive: "bg-destructive-soft text-destructive-foreground",
      info: "bg-info-soft text-info-foreground",
    },
  },
  defaultVariants: { tone: "neutral" },
});

type BadgeProps = ComponentProps<"span"> & VariantProps<typeof badgeVariants>;

export const Badge = ({ className, tone, ...props }: BadgeProps) => <span className={cn(badgeVariants({ tone }), className)} {...props} />;
