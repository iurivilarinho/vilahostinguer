import { AlertTriangle, Info, OctagonAlert } from "lucide-react";
import type { ReactNode } from "react";
import { Typography } from "@/components";
import { cn } from "@/lib/merge-classes";

type NoticeTone = "info" | "warning" | "destructive";

type NoticeProps = {
  tone: NoticeTone;
  title: string;
  children?: ReactNode;
  className?: string;
};

const TONE_CLASS: Record<NoticeTone, string> = {
  info: "border-info bg-info-soft text-info-foreground",
  warning: "border-warning bg-warning-soft text-warning-foreground",
  destructive: "border-destructive bg-destructive-soft text-destructive-foreground",
};

const TONE_ICON: Record<NoticeTone, ReactNode> = {
  info: <Info />,
  warning: <AlertTriangle />,
  destructive: <OctagonAlert />,
};

/** Aviso em destaque dentro da página (CGNAT, porta ocupada, gateway desligado). */
export const Notice = ({ tone, title, children, className }: NoticeProps) => (
  <div role={tone === "info" ? "status" : "alert"} className={cn("flex gap-3 rounded-lg border p-4 [&>svg]:size-5 [&>svg]:shrink-0", TONE_CLASS[tone], className)}>
    {TONE_ICON[tone]}
    <div className="flex flex-col gap-1">
      <Typography variant="ui-header" className="text-inherit">
        {title}
      </Typography>
      {children && (
        <Typography variant="caption" as="div" className="text-inherit">
          {children}
        </Typography>
      )}
    </div>
  </div>
);
