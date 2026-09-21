import type { Meta, StoryObj } from "@storybook/react-vite";
import { Input } from "./index";

const meta: Meta<typeof Input> = {
  title: "Formulário/Input",
  component: Input,
  args: { placeholder: "169.254.1.1" },
};

export default meta;

type Story = StoryObj<typeof Input>;

export const Padrao: Story = {};

export const ComErro: Story = { args: { "aria-invalid": true, defaultValue: "endereço inválido" } };

export const Desabilitado: Story = { args: { disabled: true, defaultValue: "root" } };
