import { Copy, RefreshCw } from "lucide-react";
import { Button, Card, Typography, notify } from "@/components";
import { formatDateTime } from "@/lib/format";
import type { InvoiceDto } from "../api";

type PixPaymentProps = {
  invoice: InvoiceDto;
  automaticPix: boolean;
  manualInstructions: string | null;
  generating: boolean;
  checking: boolean;
  onGenerate: () => void;
  onCheck: () => void;
};

/**
 * Pagamento da fatura: com Pix automático, o QR code e o copia e cola (confirmação sozinha);
 * sem ele, as instruções de pagamento e o aviso de que a confirmação é feita pela equipe.
 */
export const PixPayment = ({ invoice, automaticPix, manualInstructions, generating, checking, onGenerate, onCheck }: PixPaymentProps) => {
  const copy = async () => {
    if (invoice.pixCode) {
      await navigator.clipboard.writeText(invoice.pixCode);
      notify.success("Código Pix copiado", "Cole no app do seu banco.");
    }
  };

  if (!automaticPix) {
    return (
      <Card className="flex flex-col gap-3 p-5">
        <Typography variant="title-sm">Como pagar</Typography>
        <Typography variant="body-sm" as="p" className="whitespace-pre-line">
          {manualInstructions ?? "Fale com o suporte para receber os dados de pagamento."}
        </Typography>
        <Typography variant="caption" as="p">
          Depois de pagar, a confirmação é feita pela nossa equipe e o servidor é liberado em seguida.
        </Typography>
      </Card>
    );
  }

  if (!invoice.pixCode) {
    return (
      <Card className="flex flex-col items-start gap-3 p-5">
        <Typography variant="title-sm">Pagar com Pix</Typography>
        <Typography variant="body-sm" as="p" className="text-muted-foreground">
          Gere o código e pague pelo app do seu banco. A confirmação chega em instantes.
        </Typography>
        <Button size="lg" onClick={onGenerate} loading={generating}>
          Gerar Pix
        </Button>
      </Card>
    );
  }

  return (
    <Card className="flex flex-col gap-4 p-5 md:flex-row md:items-center">
      {invoice.pixQrBase64 && (
        <img src={`data:image/png;base64,${invoice.pixQrBase64}`} alt="QR code do Pix" className="size-48 shrink-0 self-center rounded-lg border border-border bg-white p-2" />
      )}
      <div className="flex min-w-0 flex-1 flex-col gap-3">
        <Typography variant="title-sm">Pague com Pix</Typography>
        <Typography variant="body-sm" className="text-muted-foreground">
          Abra o app do banco, escolha Pix e leia o QR code ou use o copia e cola. Vale até {formatDateTime(invoice.pixExpiresAt)}.
        </Typography>
        <Typography variant="mono" className="max-h-20 overflow-y-auto rounded-md bg-muted p-2 break-all">
          {invoice.pixCode}
        </Typography>
        <div className="flex flex-wrap gap-2">
          <Button onClick={copy}>
            <Copy />
            Copiar código
          </Button>
          <Button variant="outline" onClick={onCheck} loading={checking}>
            <RefreshCw />
            Já paguei
          </Button>
        </div>
      </div>
    </Card>
  );
};
