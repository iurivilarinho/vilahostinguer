import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../button";
import { Typography } from "../typography";
import { Dialog } from "./index";

const meta: Meta<typeof Dialog> = {
  title: "Overlay/Dialog",
  component: Dialog,
};

export default meta;

type Story = StoryObj<typeof Dialog>;

const Exemplo = () => {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Abrir</Button>
      <Dialog
        open={open}
        onOpenChange={setOpen}
        title="Saída da operação"
        description="Instalar Nginx"
        footer={<Button onClick={() => setOpen(false)}>Fechar</Button>}
      >
        <Typography variant="body-sm">Conteúdo do diálogo.</Typography>
      </Dialog>
    </>
  );
};

export const Padrao: Story = { render: () => <Exemplo /> };
