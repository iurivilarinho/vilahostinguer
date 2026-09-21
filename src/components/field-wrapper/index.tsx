import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";
import { Typography } from "../typography";

type FieldWrapperProps = {
  label: string;
  htmlFor: string;
  children: ReactNode;
  description?: string;
  error?: string;
  className?: string;
};

/** Rótulo + campo + descrição + erro. Nunca montar isso à mão numa tela. */
export const FieldWrapper = ({ label, htmlFor, children, description, error, className }: FieldWrapperProps) => (
  <div className={cn("flex min-w-0 flex-col gap-1.5", className)}>
    <Typography variant="ui-header" as="label" htmlFor={htmlFor}>
      {label}
    </Typography>
    {children}
    {error ? (
      <Typography variant="caption" as="p" className="text-destructive-foreground">
        {error}
      </Typography>
    ) : description ? (
      <Typography variant="caption" as="p">
        {description}
      </Typography>
    ) : null}
  </div>
);
