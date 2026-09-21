import type { Meta, StoryObj } from "@storybook/react-vite";
import { Input } from "../input";
import { FieldWrapper } from "./index";

const meta: Meta<typeof FieldWrapper> = {
  title: "Formulário/FieldWrapper",
  component: FieldWrapper,
};

export default meta;

type Story = StoryObj<typeof FieldWrapper>;

export const ComDescricao: Story = {
  render: () => (
    <FieldWrapper label="Endereço" htmlFor="host" description="IP ou nome do dispositivo" className="max-w-sm">
      <Input id="host" placeholder="169.254.1.1" />
    </FieldWrapper>
  ),
};

export const ComErro: Story = {
  render: () => (
    <FieldWrapper label="Usuário" htmlFor="user" error="Informe o usuário" className="max-w-sm">
      <Input id="user" aria-invalid />
    </FieldWrapper>
  ),
};
