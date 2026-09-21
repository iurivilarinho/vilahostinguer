import { Copy, Eye } from "lucide-react";
import { useEffect } from "react";
import { Button, Dialog, notify, Textarea, Typography } from "@/components";
import { useRevealCredentialMutation, type CredentialDto } from "../api";

type RevealSecretDialogProps = {
  credential: CredentialDto | null;
  onClose: () => void;
};

/** Mostra o segredo só depois de um clique explícito, e o esquece ao fechar. */
export const RevealSecretDialog = ({ credential, onClose }: RevealSecretDialogProps) => {
  const { mutate: reveal, data: secret, isPending, reset } = useRevealCredentialMutation();

  useEffect(() => {
    if (!credential) {
      reset();
    }
  }, [credential, reset]);

  const copy = async (value: string) => {
    await navigator.clipboard.writeText(value);
    notify.success("Copiado para a área de transferência");
  };

  return (
    <Dialog
      open={credential !== null}
      onOpenChange={(open) => !open && onClose()}
      title={credential ? `Segredo de "${credential.name}"` : "Segredo"}
      description="Evite revelar com a tela compartilhada."
      footer={
        <Button variant="outline" onClick={onClose}>
          Fechar
        </Button>
      }
    >
      {!secret ? (
        <div className="flex flex-col items-center gap-3 py-4">
          <Typography variant="body-sm" className="text-muted-foreground">
            O segredo fica cifrado no computador. Revelar decifra só para esta janela.
          </Typography>
          <Button onClick={() => credential && reveal(credential.id)} loading={isPending}>
            <Eye />
            Revelar
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          <Textarea readOnly value={secret.secret ?? ""} rows={credential?.authType === "PRIVATE_KEY" ? 8 : 2} className="font-mono text-xs" />
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" size="sm" onClick={() => copy(secret.secret ?? "")}>
              <Copy />
              Copiar {credential?.authType === "PRIVATE_KEY" ? "chave" : "senha"}
            </Button>
            {secret.passphrase && (
              <Button variant="outline" size="sm" onClick={() => copy(secret.passphrase ?? "")}>
                <Copy />
                Copiar senha da chave
              </Button>
            )}
          </div>
        </div>
      )}
    </Dialog>
  );
};
