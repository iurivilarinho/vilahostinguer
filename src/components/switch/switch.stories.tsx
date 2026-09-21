import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Switch } from "./index";

const meta: Meta<typeof Switch> = {
  title: "Formulário/Switch",
  component: Switch,
};

export default meta;

type Story = StoryObj<typeof Switch>;

const Controlado = () => {
  const [checked, setChecked] = useState(true);
  return <Switch checked={checked} onCheckedChange={setChecked} aria-label="Busca automática" />;
};

export const Padrao: Story = { render: () => <Controlado /> };
