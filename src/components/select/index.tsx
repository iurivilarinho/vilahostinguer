import { ChevronDown } from "lucide-react";
import type { ComponentProps } from "react";
import { cn } from "@/lib/merge-classes";
import { inputClassName } from "../input";

type SelectProps = ComponentProps<"select">;

/** Select nativo com o visual dos campos: acessível e sem dependência extra. */
export const Select = ({ className, children, ...props }: SelectProps) => (
  <div className="relative">
    <select className={cn(inputClassName, "cursor-pointer appearance-none pr-9", className)} {...props}>
      {children}
    </select>
    <ChevronDown className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-muted-foreground" />
  </div>
);
