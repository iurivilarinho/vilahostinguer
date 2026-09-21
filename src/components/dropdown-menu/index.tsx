import * as DropdownPrimitive from "@radix-ui/react-dropdown-menu";
import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";

export type DropdownMenuItem = {
  label: string;
  icon?: ReactNode;
  onSelect: () => void;
  destructive?: boolean;
  disabled?: boolean;
};

type DropdownMenuProps = {
  trigger: ReactNode;
  items: DropdownMenuItem[];
  align?: "start" | "end";
};

/** Lista de ações de um item (menu "⋯"). */
export const DropdownMenu = ({ trigger, items, align = "end" }: DropdownMenuProps) => (
  <DropdownPrimitive.Root>
    <DropdownPrimitive.Trigger asChild>{trigger}</DropdownPrimitive.Trigger>
    <DropdownPrimitive.Portal>
      <DropdownPrimitive.Content
        align={align}
        sideOffset={6}
        className="z-50 min-w-48 rounded-lg border border-border bg-popover p-1 text-popover-foreground shadow-lg"
      >
        {items.map((item) => (
          <DropdownPrimitive.Item
            key={item.label}
            onSelect={item.onSelect}
            disabled={item.disabled}
            className={cn(
              "flex cursor-pointer items-center gap-2 rounded-md px-3 py-2 text-sm outline-none select-none data-[disabled]:cursor-not-allowed data-[disabled]:opacity-50 data-[highlighted]:bg-muted [&_svg]:size-4",
              item.destructive && "text-destructive-foreground",
            )}
          >
            {item.icon}
            {item.label}
          </DropdownPrimitive.Item>
        ))}
      </DropdownPrimitive.Content>
    </DropdownPrimitive.Portal>
  </DropdownPrimitive.Root>
);
