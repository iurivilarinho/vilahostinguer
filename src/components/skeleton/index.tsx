import type { ComponentProps } from "react";
import { cn } from "@/lib/merge-classes";

type SkeletonProps = ComponentProps<"div">;

export const Skeleton = ({ className, ...props }: SkeletonProps) => (
  <div className={cn("animate-pulse rounded-md bg-muted", className)} aria-hidden {...props} />
);
