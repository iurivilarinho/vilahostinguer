import { useState, type ReactNode } from "react";
import { Button } from "../button";
import { Dialog } from "../dialog";
import { Input } from "../input";
import { Typography } from "../typography";

type ConfirmDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children?: ReactNode;
  confirmLabel: string;
  onConfirm: () => void;
  destructive?: boolean;
  loading?: boolean;
  /** Quando informado, a confirmação só libera depois de a pessoa digitar exatamente este texto. */
  typeToConfirm?: string;
};

export const ConfirmDialog = ({
  open,
  onOpenChange,
  title,
  description,
  children,
  confirmLabel,
  onConfirm,
  destructive = false,
  loading = false,
  typeToConfirm,
}: ConfirmDialogProps) => {
  const [typed, setTyped] = useState("");
  const blocked = typeToConfirm !== undefined && typed.trim() !== typeToConfirm;

  const handleOpenChange = (next: boolean) => {
    if (!next) {
      setTyped("");
    }
    onOpenChange(next);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={handleOpenChange}
      title={title}
      description={description}
      footer={
        <>
          <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={loading}>
            Cancelar
          </Button>
          <Button variant={destructive ? "destructive" : "primary"} onClick={onConfirm} disabled={blocked} loading={loading}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {children}
        {typeToConfirm !== undefined && (
          <div className="flex flex-col gap-2">
            <Typography variant="body-sm">
              Digite <strong className="font-mono">{typeToConfirm}</strong> para confirmar.
            </Typography>
            <Input value={typed} onChange={(event) => setTyped(event.target.value)} aria-label="Confirmação" autoFocus />
          </div>
        )}
      </div>
    </Dialog>
  );
};
