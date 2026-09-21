import type { ComponentProps } from "react";
import { cn } from "@/lib/merge-classes";

type TableProps = ComponentProps<"table">;
type TableHeaderProps = ComponentProps<"thead">;
type TableBodyProps = ComponentProps<"tbody">;
type TableRowProps = ComponentProps<"tr">;
type TableHeadProps = ComponentProps<"th">;
type TableCellProps = ComponentProps<"td">;

export const Table = ({ className, ...props }: TableProps) => (
  <div className="w-full overflow-x-auto">
    <table className={cn("w-full border-collapse text-sm", className)} {...props} />
  </div>
);

export const TableHeader = ({ className, ...props }: TableHeaderProps) => <thead className={cn("bg-muted/60", className)} {...props} />;

export const TableBody = ({ className, ...props }: TableBodyProps) => <tbody className={cn("[&_tr:last-child]:border-0", className)} {...props} />;

export const TableRow = ({ className, ...props }: TableRowProps) => (
  <tr className={cn("border-b border-border transition-colors hover:bg-muted/40", className)} {...props} />
);

export const TableHead = ({ className, ...props }: TableHeadProps) => (
  <th className={cn("h-10 px-4 text-left align-middle text-xs font-semibold whitespace-nowrap text-muted-foreground", className)} {...props} />
);

export const TableCell = ({ className, ...props }: TableCellProps) => <td className={cn("px-4 py-3 align-middle", className)} {...props} />;
