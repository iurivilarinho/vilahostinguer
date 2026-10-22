import { Eye, EyeOff } from "lucide-react";
import { useCallback, useEffect, useId, useRef, useState, type ComponentProps } from "react";
import { Input } from "@/components/input";
import { cn } from "@/lib/merge-classes";

type PasswordInputProps = Omit<ComponentProps<"input">, "type">;

/**
 * Password field with exactly one reveal toggle (PLT-033, standard 030). Microsoft Edge draws its
 * own button; it is hidden in index.css so only this one shows. The field hides again on submit.
 */
export const PasswordInput = ({ id, className, ref, ...props }: PasswordInputProps) => {
  const [isVisible, setIsVisible] = useState(false);
  const innerRef = useRef<HTMLInputElement | null>(null);
  const fallbackId = useId();
  const inputId = id ?? fallbackId;

  const setRefs = useCallback(
    (node: HTMLInputElement | null) => {
      innerRef.current = node;
      if (typeof ref === "function") ref(node);
      else if (ref) ref.current = node;
    },
    [ref],
  );

  useEffect(() => {
    const form = innerRef.current?.form;
    if (!form) return;
    const hide = () => setIsVisible(false);
    form.addEventListener("submit", hide);
    return () => form.removeEventListener("submit", hide);
  }, []);

  return (
    <div className="relative">
      <Input {...props} id={inputId} ref={setRefs} type={isVisible ? "text" : "password"} className={cn("pr-11", className)} />
      <button
        type="button"
        onClick={() => setIsVisible((previous) => !previous)}
        className="absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground hover:text-foreground"
        aria-label={isVisible ? "Ocultar senha" : "Mostrar senha"}
        aria-pressed={isVisible}
        aria-controls={inputId}
      >
        {isVisible ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
      </button>
    </div>
  );
};
