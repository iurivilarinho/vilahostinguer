import type { Meta, StoryObj } from "@storybook/react-vite";
import { Plus } from "lucide-react";
import { Button } from "../button";
import { PageHeader } from "./index";

const meta: Meta<typeof PageHeader> = {
  title: "Navegação/PageHeader",
  component: PageHeader,
};

export default meta;

type Story = StoryObj<typeof PageHeader>;

export const ComAcoes: Story = {
  render: () => (
    <PageHeader
      title="Dispositivos"
      description="Aparelhos conectados por cabo ou pela rede"
      actions={
        <Button>
          <Plus />
          Adicionar
        </Button>
      }
    />
  ),
};
