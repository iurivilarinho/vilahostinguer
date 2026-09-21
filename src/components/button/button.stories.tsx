import type { Meta, StoryObj } from "@storybook/react-vite";
import { Plus, RefreshCw, Trash2 } from "lucide-react";
import { Button } from "./index";

const meta: Meta<typeof Button> = {
  title: "Base/Button",
  component: Button,
  args: { children: "Salvar" },
};

export default meta;

type Story = StoryObj<typeof Button>;

export const Primario: Story = {};

export const Variantes: Story = {
  render: () => (
    <div className="flex flex-wrap gap-3">
      <Button>
        <Plus />
        Adicionar
      </Button>
      <Button variant="secondary">Secundário</Button>
      <Button variant="outline">
        <RefreshCw />
        Atualizar
      </Button>
      <Button variant="ghost">Fantasma</Button>
      <Button variant="destructive">
        <Trash2 />
        Apagar
      </Button>
      <Button variant="link">Link</Button>
      <Button loading>Salvando</Button>
    </div>
  ),
};
