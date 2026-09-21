import type { Meta, StoryObj } from "@storybook/react-vite";
import { HardDrive, Server } from "lucide-react";
import { StatCard } from "./index";

const meta: Meta<typeof StatCard> = {
  title: "Dados/StatCard",
  component: StatCard,
};

export default meta;

type Story = StoryObj<typeof StatCard>;

export const Varios: Story = {
  render: () => (
    <div className="grid max-w-2xl grid-cols-2 gap-4">
      <StatCard label="Dispositivos online" value="2 de 3" icon={<Server />} tone="success" />
      <StatCard label="Backups" value="7" icon={<HardDrive />} hint="Último há 2 horas" />
    </div>
  ),
};
