import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";

type OptionCardProps = {
  selected: boolean;
  onSelect: () => void;
  disabled?: boolean;
  children: ReactNode;
  className?: string;
};

/** Cartão selecionável (período, sistema operacional) com o destaque roxo do item escolhido. */
export const OptionCard = ({ selected, onSelect, disabled = false, children, className }: OptionCardProps) => (
  <button
    type="button"
    role="radio"
    aria-checked={selected}
    disabled={disabled}
    onClick={onSelect}
    className={cn(
      "flex cursor-pointer flex-col gap-1 rounded-lg border border-border bg-card p-4 text-left transition-colors hover:border-primary disabled:cursor-not-allowed disabled:opacity-50",
      selected && "border-2 border-primary bg-primary-soft/40",
      className,
    )}
  >
    {children}
  </button>
);
