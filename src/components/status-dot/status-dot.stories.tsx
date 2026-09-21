import type { Meta, StoryObj } from "@storybook/react-vite";
import { StatusDot } from "./index";

const meta: Meta<typeof StatusDot> = {
  title: "Dados/StatusDot",
  component: StatusDot,
};

export default meta;

type Story = StoryObj<typeof StatusDot>;

export const Online: Story = { args: { online: true } };

export const Desconectado: Story = { args: { online: false } };
