import type { Meta, StoryObj } from "@storybook/react-vite";
import { Progress } from "./index";

const meta: Meta<typeof Progress> = {
  title: "Dados/Progress",
  component: Progress,
};

export default meta;

type Story = StoryObj<typeof Progress>;

export const Faixas: Story = {
  render: () => (
    <div className="flex max-w-sm flex-col gap-4">
      <Progress value={32} aria-label="CPU" />
      <Progress value={80} aria-label="Memória" />
      <Progress value={95} aria-label="Disco" />
    </div>
  ),
};
