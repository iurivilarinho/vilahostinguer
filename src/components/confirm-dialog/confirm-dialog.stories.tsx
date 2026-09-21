import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../button";
import { ConfirmDialog } from "./index";

const meta: Meta<typeof ConfirmDialog> = {
  title: "Overlay/ConfirmDialog",
  component: ConfirmDialog,
};

export default meta;

type Story = StoryObj<typeof ConfirmDialog>;

const Exemplo = ({ typeToConfirm }: { typeToConfirm?: string }) => {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button variant="destructive" onClick={() => setOpen(true)}>
        Formatar
      </Button>
      <ConfirmDialog
        open={open}
        onOpenChange={setOpen}
        title="Formatar mmcblk0p53?"
        description="Todos os dados da partição serão apagados."
        confirmLabel="Formatar"
        destructive
        typeToConfirm={typeToConfirm}
        onConfirm={() => setOpen(false)}
      />
    </>
  );
};

export const Simples: Story = { render: () => <Exemplo /> };

export const DigitarParaConfirmar: Story = { render: () => <Exemplo typeToConfirm="mmcblk0p53" /> };
