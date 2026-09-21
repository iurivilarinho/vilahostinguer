import type { Meta, StoryObj } from "@storybook/react-vite";
import { Select } from "./index";

const meta: Meta<typeof Select> = {
  title: "Formulário/Select",
  component: Select,
};

export default meta;

type Story = StoryObj<typeof Select>;

export const Padrao: Story = {
  render: () => (
    <Select defaultValue="PASSWORD" className="max-w-xs">
      <option value="PASSWORD">Senha</option>
      <option value="PRIVATE_KEY">Chave privada</option>
    </Select>
  ),
};
