import { HardDrive, Plus, Plug, RefreshCw, Server, Trash2 } from "lucide-react";
import { useState, type ReactNode } from "react";
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  ConfirmDialog,
  EmptyState,
  FieldWrapper,
  Input,
  LogViewer,
  notify,
  PageHeader,
  Progress,
  QueryErrorState,
  Select,
  Skeleton,
  StatCard,
  StatusDot,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  Textarea,
  Typography,
  type TypographyVariant,
} from "@/components";

const COLOR_TOKENS = [
  { name: "primary", className: "bg-primary" },
  { name: "primary-soft", className: "bg-primary-soft" },
  { name: "heading", className: "bg-heading" },
  { name: "background", className: "bg-background" },
  { name: "card", className: "bg-card" },
  { name: "muted", className: "bg-muted" },
  { name: "border", className: "bg-border" },
  { name: "success", className: "bg-success" },
  { name: "warning", className: "bg-warning" },
  { name: "destructive", className: "bg-destructive" },
  { name: "info", className: "bg-info" },
  { name: "terminal", className: "bg-terminal" },
];

const TYPE_SCALE: TypographyVariant[] = ["display-md", "display-sm", "title-lg", "title-md", "title-sm", "body-md", "body-sm", "caption", "section-label", "mono"];

type SectionProps = {
  number: string;
  title: string;
  children: ReactNode;
};

const Section = ({ number, title, children }: SectionProps) => (
  <section className="flex flex-col gap-4">
    <div className="flex flex-col gap-1">
      <Typography variant="section-label">{number}</Typography>
      <Typography variant="title-lg" as="h2">
        {title}
      </Typography>
    </div>
    {children}
  </section>
);

/** Vitrine dos componentes reutilizáveis: toda peça nova de `src/components` ganha um exemplo aqui. */
export const PageDesignSystem = () => {
  const [tab, setTab] = useState<"um" | "dois">("um");
  const [checked, setChecked] = useState(true);
  const [confirmOpen, setConfirmOpen] = useState(false);

  return (
    <>
      <PageHeader title="Design system" description="Linguagem visual do painel: roxo como cor de ação, cartões brancos, DM Sans." />

      <Section number="01 — CORES" title="Tokens">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6">
          {COLOR_TOKENS.map((token) => (
            <div key={token.name} className="flex flex-col gap-2">
              <div className={`h-14 rounded-lg border border-border ${token.className}`} />
              <Typography variant="mono">{token.name}</Typography>
            </div>
          ))}
        </div>
      </Section>

      <Section number="02 — TIPOGRAFIA" title="Escala">
        <Card>
          <CardContent className="flex flex-col gap-3">
            {TYPE_SCALE.map((variant) => (
              <div key={variant} className="flex items-baseline gap-4">
                <Typography variant="mono" className="w-32 shrink-0 text-muted-foreground">
                  {variant}
                </Typography>
                <Typography variant={variant}>Servidor caseiro no cabo USB</Typography>
              </div>
            ))}
          </CardContent>
        </Card>
      </Section>

      <Section number="03 — AÇÕES" title="Botões e badges">
        <div className="flex flex-wrap gap-3">
          <Button>
            <Plus />
            Primário
          </Button>
          <Button variant="secondary">Secundário</Button>
          <Button variant="outline">
            <RefreshCw />
            Contorno
          </Button>
          <Button variant="ghost">Fantasma</Button>
          <Button variant="destructive">
            <Trash2 />
            Destrutivo
          </Button>
          <Button loading>Carregando</Button>
        </div>
        <div className="flex flex-wrap gap-2">
          <Badge>Neutro</Badge>
          <Badge tone="primary">Cabo USB</Badge>
          <Badge tone="success">
            <StatusDot online />
            Online
          </Badge>
          <Badge tone="warning">Detectado</Badge>
          <Badge tone="destructive">Falhou</Badge>
          <Badge tone="info">Em execução</Badge>
        </div>
      </Section>

      <Section number="04 — DADOS" title="Indicadores">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <StatCard label="Dispositivos online" value="2 de 3" icon={<Server />} tone="success" />
          <StatCard label="Backups" value="7" icon={<HardDrive />} hint="Último há 2 horas" />
          <Card className="flex flex-col gap-3 p-5">
            <Typography variant="caption">Uso</Typography>
            <Progress value={35} aria-label="Baixo" />
            <Progress value={80} aria-label="Alto" />
            <Progress value={95} aria-label="Crítico" />
          </Card>
        </div>
        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Operação</TableHead>
                <TableHead>Situação</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow>
                <TableCell>Instalar Nginx</TableCell>
                <TableCell>
                  <Badge tone="success">Concluída</Badge>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </Card>
        <LogViewer text={"== Instalando Nginx ==\n(1/1) Installing nginx (1.26.2-r0)\n== Concluído =="} />
      </Section>

      <Section number="05 — FORMULÁRIOS" title="Campos">
        <Card>
          <CardHeader>
            <Typography variant="title-md">Nova credencial</Typography>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <FieldWrapper label="Nome" htmlFor="ds-name" description="Para reconhecer na lista">
              <Input id="ds-name" placeholder="Root dos celulares" />
            </FieldWrapper>
            <FieldWrapper label="Usuário" htmlFor="ds-user" error="Informe o usuário">
              <Input id="ds-user" aria-invalid />
            </FieldWrapper>
            <FieldWrapper label="Autenticação" htmlFor="ds-auth">
              <Select id="ds-auth">
                <option>Senha</option>
                <option>Chave privada</option>
              </Select>
            </FieldWrapper>
            <div className="flex items-center gap-3">
              <Switch checked={checked} onCheckedChange={setChecked} aria-label="Padrão" />
              <Typography variant="body-sm">Credencial padrão</Typography>
            </div>
            <FieldWrapper label="Chave" htmlFor="ds-key" className="md:col-span-2">
              <Textarea id="ds-key" className="font-mono text-xs" placeholder="-----BEGIN OPENSSH PRIVATE KEY-----" />
            </FieldWrapper>
          </CardContent>
          <CardFooter>
            <Button variant="outline">Cancelar</Button>
            <Button>Salvar</Button>
          </CardFooter>
        </Card>
      </Section>

      <Section number="06 — NAVEGAÇÃO E FEEDBACK" title="Estados">
        <Tabs
          value={tab}
          onValueChange={setTab}
          items={[
            { value: "um", label: "Ativas" },
            { value: "dois", label: "Arquivadas" },
          ]}
        />
        <div className="flex flex-wrap gap-3">
          <Button variant="outline" onClick={() => notify.success("Credencial guardada")}>
            Toast de sucesso
          </Button>
          <Button variant="outline" onClick={() => notify.error("Não foi possível conectar")}>
            Toast de erro
          </Button>
          <Button variant="outline" onClick={() => setConfirmOpen(true)}>
            Confirmação digitada
          </Button>
        </div>
        <div className="grid gap-4 lg:grid-cols-3">
          <Card>
            <EmptyState icon={<Plug />} title="Nenhum dispositivo" description="Conecte um aparelho pelo cabo USB." />
          </Card>
          <QueryErrorState message="O dispositivo não respondeu." onRetry={() => undefined} />
          <Card className="flex flex-col gap-3 p-5">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
          </Card>
        </div>
      </Section>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Formatar mmcblk0p53?"
        description="Todos os dados da partição serão apagados."
        confirmLabel="Formatar"
        destructive
        typeToConfirm="mmcblk0p53"
        onConfirm={() => setConfirmOpen(false)}
      />
    </>
  );
};
