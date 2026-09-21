import { useEffect, useRef } from "react";
import { cn } from "@/lib/merge-classes";

type LogViewerProps = {
  text: string;
  follow?: boolean;
  className?: string;
};

/** Saída de comando em fonte monoespaçada. Com `follow`, acompanha o fim enquanto a operação roda. */
export const LogViewer = ({ text, follow = false, className }: LogViewerProps) => {
  const containerRef = useRef<HTMLPreElement>(null);

  useEffect(() => {
    if (follow && containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [text, follow]);

  return (
    <pre
      ref={containerRef}
      className={cn(
        "max-h-[55vh] min-h-40 overflow-auto rounded-lg bg-terminal p-4 font-mono text-xs leading-relaxed whitespace-pre-wrap text-terminal-foreground",
        className,
      )}
    >
      {text || "Aguardando saída…"}
    </pre>
  );
};
