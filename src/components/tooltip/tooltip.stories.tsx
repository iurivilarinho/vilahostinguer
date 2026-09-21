import type { Meta, StoryObj } from "@storybook/react-vite";
import { RefreshCw } from "lucide-react";
import { Button } from "../button";
import { Tooltip } from "./index";

const meta: Meta<typeof Tooltip> = {
  title: "Overlay/Tooltip",
  component: Tooltip,
};

export default meta;

type Story = StoryObj<typeof Tooltip>;

export const Padrao: Story = {
  render: () => (
    <Tooltip content="Procurar dispositivos agora">
      <Button variant="outline" size="icon" aria-label="Procurar">
        <RefreshCw />
      </Button>
    </Tooltip>
  ),
};
