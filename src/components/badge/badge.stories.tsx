import type { Meta, StoryObj } from "@storybook/react-vite";
import { Badge } from "./index";

const meta: Meta<typeof Badge> = {
  title: "Base/Badge",
  component: Badge,
};

export default meta;

type Story = StoryObj<typeof Badge>;

export const Tons: Story = {
  render: () => (
    <div className="flex flex-wrap gap-2">
      <Badge>Neutro</Badge>
      <Badge tone="primary">Cabo USB</Badge>
      <Badge tone="success">Online</Badge>
      <Badge tone="warning">Detectado</Badge>
      <Badge tone="destructive">Falhou</Badge>
      <Badge tone="info">Em execução</Badge>
    </div>
  ),
};
