import { Upload } from "lucide-react";
import { useRef, type ChangeEvent } from "react";
import { cn } from "@/lib/merge-classes";
import { buttonVariants } from "../button";

type FileInputProps = {
  onFilesSelected: (files: File[]) => void;
  label?: string;
  multiple?: boolean;
  disabled?: boolean;
  className?: string;
};

/** Seletor de arquivo com cara de botão. Nunca usar `<input type="file">` direto nas telas. */
const FileInput = ({ onFilesSelected, label = "Enviar arquivo", multiple = false, disabled = false, className }: FileInputProps) => {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    if (files.length > 0) {
      onFilesSelected(files);
    }
    event.target.value = "";
  };

  return (
    <>
      <button
        type="button"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        className={cn(buttonVariants({ variant: "outline", size: "md" }), className)}
      >
        <Upload />
        {label}
      </button>
      <input ref={inputRef} type="file" className="hidden" multiple={multiple} onChange={handleChange} />
    </>
  );
};

export const FileUI = {
  Input: FileInput,
};
