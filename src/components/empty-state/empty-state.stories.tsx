import type { Meta, StoryObj } from "@storybook/react-vite";
import { Plug } from "lucide-react";
import { Button } from "../button";
import { EmptyState } from "./index";

const meta: Meta<typeof EmptyState> = {
  title: "Feedback/EmptyState",
  component: EmptyState,
};

export default meta;

type Story = StoryObj<typeof EmptyState>;

export const Padrao: Story = {
  render: () => (
    <EmptyState
      icon={<Plug />}
      title="Nenhum dispositivo ainda"
      description="Conecte um aparelho pelo cabo USB. Ele aparece aqui sozinho em alguns segundos."
      action={<Button variant="outline">Adicionar pelo endereço</Button>}
    />
  ),
};
