import type { Meta, StoryObj } from "@storybook/react-vite";
import { Archive, MoreHorizontal, Pencil } from "lucide-react";
import { Button } from "../button";
import { DropdownMenu } from "./index";

const meta: Meta<typeof DropdownMenu> = {
  title: "Overlay/DropdownMenu",
  component: DropdownMenu,
};

export default meta;

type Story = StoryObj<typeof DropdownMenu>;

export const Padrao: Story = {
  render: () => (
    <DropdownMenu
      trigger={
        <Button variant="ghost" size="icon" aria-label="Ações">
          <MoreHorizontal />
        </Button>
      }
      items={[
        { label: "Editar", icon: <Pencil />, onSelect: () => undefined },
        { label: "Arquivar", icon: <Archive />, onSelect: () => undefined, destructive: true },
      ]}
    />
  ),
};
