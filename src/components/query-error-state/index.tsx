import { AlertTriangle, RotateCw } from "lucide-react";
import { cn } from "@/lib/merge-classes";
import { Button } from "../button";
import { Typography } from "../typography";

type QueryErrorStateProps = {
  message: string;
  onRetry?: () => void;
  retrying?: boolean;
  className?: string;
};

export const QueryErrorState = ({ message, onRetry, retrying = false, className }: QueryErrorStateProps) => (
  <div className={cn("flex flex-col items-center gap-3 rounded-lg border border-destructive-soft bg-destructive-soft/40 px-6 py-8 text-center", className)}>
    <AlertTriangle className="size-6 text-destructive-foreground" />
    <Typography variant="body-sm" className="max-w-md">
      {message}
    </Typography>
    {onRetry && (
      <Button variant="outline" size="sm" onClick={onRetry} loading={retrying}>
        <RotateCw />
        Tentar de novo
      </Button>
    )}
  </div>
);
