import { useEffect, useState } from "react";
import { ConfirmDialog, FieldWrapper, Textarea } from "@/components";

type ReasonDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  destructive?: boolean;
  loading: boolean;
  typeToConfirm?: string;
  onClose: () => void;
  onConfirm: (reason: string) => void;
};

/** Confirmação com justificativa: o motivo vai para a auditoria. */
export const ReasonDialog = ({ open, title, description, confirmLabel, destructive = false, loading, typeToConfirm, onClose, onConfirm }: ReasonDialogProps) => {
  const [reason, setReason] = useState("");

  useEffect(() => {
    if (open) {
      setReason("");
    }
  }, [open]);

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={(value) => !value && onClose()}
      title={title}
      description={description}
      confirmLabel={confirmLabel}
      destructive={destructive}
      loading={loading}
      typeToConfirm={typeToConfirm}
      onConfirm={() => onConfirm(reason)}
    >
      <FieldWrapper label="Motivo (fica na auditoria)" htmlFor="reason-text">
        <Textarea id="reason-text" rows={2} value={reason} onChange={(event) => setReason(event.target.value)} />
      </FieldWrapper>
    </ConfirmDialog>
  );
};
