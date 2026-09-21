import { ChevronLeft, ChevronRight } from "lucide-react";
import type { TablePagination } from "@/lib/api/types";
import { cn } from "@/lib/merge-classes";
import { Button } from "../button";
import { Typography } from "../typography";

const PAGE_SIZES = [10, 25, 50];

type TableFooterProps = {
  pagination: TablePagination;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
  className?: string;
};

/** Paginação das tabelas. `pagination.page` é 0-based; só o texto mostra page + 1. */
export const TableFooter = ({ pagination, onPageChange, onSizeChange, className }: TableFooterProps) => {
  const { page, size, totalElements, totalPages } = pagination;
  const lastPage = Math.max(totalPages - 1, 0);
  return (
    <div className={cn("flex flex-wrap items-center justify-between gap-3 border-t border-border px-4 py-3", className)}>
      <Typography variant="caption">
        {totalElements} {totalElements === 1 ? "registro" : "registros"}
      </Typography>
      <div className="flex items-center gap-3">
        <label className="flex items-center gap-2">
          <Typography variant="caption">Por página</Typography>
          <select
            className="h-8 cursor-pointer rounded-md border border-input bg-card px-2 text-sm"
            value={size}
            onChange={(event) => onSizeChange(Number(event.target.value))}
          >
            {PAGE_SIZES.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <Typography variant="caption">
          {totalPages === 0 ? 0 : page + 1} de {totalPages}
        </Typography>
        <Button variant="outline" size="icon" aria-label="Página anterior" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft />
        </Button>
        <Button variant="outline" size="icon" aria-label="Próxima página" disabled={page >= lastPage} onClick={() => onPageChange(page + 1)}>
          <ChevronRight />
        </Button>
      </div>
    </div>
  );
};
