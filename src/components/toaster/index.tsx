import { Toaster as SonnerToaster, toast } from "sonner";

/** Toasts com os tokens do projeto. Um só, montado no provider da aplicação. */
export const Toaster = () => (
  <SonnerToaster
    position="bottom-right"
    toastOptions={{
      classNames: {
        toast: "!bg-popover !border-border !text-popover-foreground !rounded-lg !shadow-lg !font-sans",
        description: "!text-muted-foreground",
        success: "[&_[data-icon]]:!text-success",
        error: "[&_[data-icon]]:!text-destructive",
        warning: "[&_[data-icon]]:!text-warning-foreground",
        info: "[&_[data-icon]]:!text-info",
      },
    }}
  />
);

export const notify = {
  success: (message: string, description?: string) => toast.success(message, { description }),
  error: (message: string, description?: string) => toast.error(message, { description }),
  warning: (message: string, description?: string) => toast.warning(message, { description }),
  info: (message: string, description?: string) => toast.info(message, { description }),
};
