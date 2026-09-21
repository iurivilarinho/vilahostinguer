import type { Meta, StoryObj } from "@storybook/react-vite";
import { Textarea } from "./index";

const meta: Meta<typeof Textarea> = {
  title: "Formulário/Textarea",
  component: Textarea,
  args: { placeholder: "-----BEGIN OPENSSH PRIVATE KEY-----" },
};

export default meta;

type Story = StoryObj<typeof Textarea>;

export const Padrao: Story = {};

export const Monoespacado: Story = { args: { className: "font-mono text-xs" } };
