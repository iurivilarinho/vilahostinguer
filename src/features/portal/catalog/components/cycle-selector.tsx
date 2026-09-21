import type { BillingCycle } from "../api";
import { cn } from "@/lib/merge-classes";

const CYCLES: { value: BillingCycle; label: string; badge?: string }[] = [
  { value: "MONTHLY", label: "Mensal" },
  { value: "QUARTERLY", label: "3 meses", badge: "-5%" },
  { value: "SEMIANNUAL", label: "6 meses", badge: "-10%" },
  { value: "ANNUAL", label: "12 meses", badge: "-20%" },
];

type CycleSelectorProps = {
  value: BillingCycle;
  onChange: (cycle: BillingCycle) => void;
};

/** Escolha do período de cobrança; os períodos longos mostram o desconto. */
export const CycleSelector = ({ value, onChange }: CycleSelectorProps) => (
  <div role="radiogroup" aria-label="Período" className="inline-flex flex-wrap gap-1 rounded-full border border-border bg-card p-1">
    {CYCLES.map((cycle) => (
      <button
        key={cycle.value}
        type="button"
        role="radio"
        aria-checked={value === cycle.value}
        onClick={() => onChange(cycle.value)}
        className={cn(
          "flex cursor-pointer items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold text-muted-foreground transition-colors hover:text-foreground",
          value === cycle.value && "bg-primary text-primary-foreground hover:text-primary-foreground",
        )}
      >
        {cycle.label}
        {cycle.badge && (
          <span className={cn("rounded-full bg-success-soft px-2 py-0.5 text-xs text-success-foreground", value === cycle.value && "bg-white/20 text-white")}>
            {cycle.badge}
          </span>
        )}
      </button>
    ))}
  </div>
);
