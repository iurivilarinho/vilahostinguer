import { CheckCircle2, Circle } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, Typography } from "@/components";
import { Rotas } from "@/app/variables/rotas";
import { cn } from "@/lib/merge-classes";

type Step = {
  done: boolean;
  title: string;
  description: string;
  to?: string;
  linkLabel?: string;
};

type GettingStartedProps = {
  hasDefaultCredential: boolean;
  hasDevices: boolean;
  hasReadyDevice: boolean;
};

/** Passo a passo da primeira vez; some quando tudo estiver feito. */
export const GettingStarted = ({ hasDefaultCredential, hasDevices, hasReadyDevice }: GettingStartedProps) => {
  const steps: Step[] = [
    {
      done: hasDefaultCredential,
      title: "Cadastre a credencial padrão",
      description: "Usuário e senha (ou chave) dos seus aparelhos. Dispositivos novos usam ela sozinhos.",
      to: Rotas.credentials,
      linkLabel: "Ir para Credenciais",
    },
    {
      done: hasDevices,
      title: "Conecte um dispositivo",
      description: "Ligue o aparelho pelo cabo USB com a rede USB e o SSH ativos. Ele aparece aqui em segundos.",
      to: Rotas.devices.list,
      linkLabel: "Ver dispositivos",
    },
    {
      done: hasReadyDevice,
      title: "Use o painel",
      description: "Terminal, aplicativos (nginx, Node, Java), arquivos, backups e armazenamento ficam liberados.",
    },
  ];

  if (steps.every((step) => step.done)) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-col gap-1">
          <Typography variant="title-md">Primeiros passos</Typography>
          <Typography variant="body-sm" className="text-muted-foreground">
            {steps.filter((step) => step.done).length} de {steps.length} concluídos
          </Typography>
        </div>
      </CardHeader>
      <CardContent className="grid gap-4 md:grid-cols-3">
        {steps.map((step) => (
          <div key={step.title} className={cn("flex gap-3 rounded-lg border border-border p-4", step.done && "bg-success-soft/40")}>
            {step.done ? <CheckCircle2 className="size-5 shrink-0 text-success" /> : <Circle className="size-5 shrink-0 text-muted-foreground" />}
            <div className="flex flex-col gap-1">
              <Typography variant="title-sm">{step.title}</Typography>
              <Typography variant="body-sm" className="text-muted-foreground">
                {step.description}
              </Typography>
              {!step.done && step.to && (
                <Link to={step.to} className="inline-link pt-1 text-sm">
                  {step.linkLabel}
                </Link>
              )}
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
};
