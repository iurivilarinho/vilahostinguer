import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Tabs } from "./index";

const meta: Meta = {
  title: "Navegação/Tabs",
};

export default meta;

type Story = StoryObj;

const Exemplo = () => {
  const [value, setValue] = useState<"todos" | "online" | "offline">("todos");
  return (
    <Tabs
      value={value}
      onValueChange={setValue}
      items={[
        { value: "todos", label: "Todos" },
        { value: "online", label: "Online" },
        { value: "offline", label: "Desconectados" },
      ]}
    />
  );
};

export const Padrao: Story = { render: () => <Exemplo /> };
