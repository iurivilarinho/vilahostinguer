import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/merge-classes";
import { Typography } from "../typography";

type DialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children?: ReactNode;
  footer?: ReactNode;
  className?: string;
};

/** Decisão bloqueante ou conteúdo auxiliar. Formulários de cadastro usam AppSheet. */
export const Dialog = ({ open, onOpenChange, title, description, children, footer, className }: DialogProps) => (
  <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-overlay" />
      <DialogPrimitive.Content
        className={cn(
          "fixed top-1/2 left-1/2 z-50 flex max-h-[90vh] w-[calc(100vw-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 flex-col rounded-2xl border border-border bg-popover text-popover-foreground shadow-xl focus:outline-none",
          className,
        )}
      >
        <header className="flex items-start justify-between gap-4 px-6 pt-5 pb-3">
          <div className="flex flex-col gap-1">
            <DialogPrimitive.Title asChild>
              <Typography variant="title-md">{title}</Typography>
            </DialogPrimitive.Title>
            {description ? (
              <DialogPrimitive.Description asChild>
                <Typography variant="body-sm" className="text-muted-foreground">
                  {description}
                </Typography>
              </DialogPrimitive.Description>
            ) : (
              <DialogPrimitive.Description className="sr-only">{title}</DialogPrimitive.Description>
            )}
          </div>
          <DialogPrimitive.Close className="rounded-md p-1 text-muted-foreground hover:bg-muted hover:text-foreground" aria-label="Fechar">
            <X className="size-4" />
          </DialogPrimitive.Close>
        </header>
        {children && <div className="min-h-0 flex-1 overflow-y-auto px-6 pb-4">{children}</div>}
        {footer && <footer className="flex justify-end gap-3 border-t border-border px-6 py-4">{footer}</footer>}
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  </DialogPrimitive.Root>
);
