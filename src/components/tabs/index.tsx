import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";

export type TabItem<T extends string> = {
  value: T;
  label: string;
  icon?: ReactNode;
};

type TabsProps<T extends string> = {
  items: TabItem<T>[];
  value: T;
  onValueChange: (value: T) => void;
  className?: string;
};

export const Tabs = <T extends string>({ items, value, onValueChange, className }: TabsProps<T>) => (
  <div role="tablist" className={cn("flex gap-1 border-b border-border", className)}>
    {items.map((item) => (
      <button
        key={item.value}
        type="button"
        role="tab"
        aria-selected={item.value === value}
        onClick={() => onValueChange(item.value)}
        className={cn(
          "nav-link -mb-px inline-flex cursor-pointer items-center gap-2 border-b-2 border-transparent px-3 py-2.5 text-muted-foreground transition-colors hover:text-foreground [&_svg]:size-4",
          item.value === value && "border-primary text-primary",
        )}
      >
        {item.icon}
        {item.label}
      </button>
    ))}
  </div>
);
