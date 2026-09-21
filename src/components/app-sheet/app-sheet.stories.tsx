import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Button } from "../button";
import { FieldWrapper } from "../field-wrapper";
import { Input } from "../input";
import { AppSheet } from "./index";

const meta: Meta<typeof AppSheet> = {
  title: "Overlay/AppSheet",
  component: AppSheet,
};

export default meta;

type Story = StoryObj<typeof AppSheet>;

const Exemplo = () => {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Nova credencial</Button>
      <AppSheet
        open={open}
        onOpenChange={setOpen}
        title="Nova credencial"
        description="Guardada cifrada neste computador"
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancelar
            </Button>
            <Button>Salvar</Button>
          </>
        }
      >
        <FieldWrapper label="Nome" htmlFor="nome">
          <Input id="nome" />
        </FieldWrapper>
      </AppSheet>
    </>
  );
};

export const Padrao: Story = { render: () => <Exemplo /> };
