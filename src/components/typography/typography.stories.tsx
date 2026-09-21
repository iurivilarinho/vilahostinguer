import type { Meta, StoryObj } from "@storybook/react-vite";
import { Typography } from "./index";

const meta: Meta<typeof Typography> = {
  title: "Base/Typography",
  component: Typography,
};

export default meta;

type Story = StoryObj<typeof Typography>;

export const Escala: Story = {
  render: () => (
    <div className="flex flex-col gap-3">
      <Typography variant="display-sm">Seus dispositivos</Typography>
      <Typography variant="title-lg">Galaxy J4+</Typography>
      <Typography variant="title-md">Recursos</Typography>
      <Typography variant="title-sm">Memória</Typography>
      <Typography variant="body-md">Texto corrido do painel.</Typography>
      <Typography variant="body-sm">Texto secundário.</Typography>
      <Typography variant="caption">Legenda</Typography>
      <Typography variant="section-label">01 — Seção</Typography>
      <Typography variant="mono">SHA256:oAKAiG/PSaXVjAKW</Typography>
    </div>
  ),
};
