import type { Meta, StoryObj } from "@storybook/react-vite";
import { Button } from "../button";
import { Toaster, notify } from "./index";

const meta: Meta<typeof Toaster> = {
  title: "Feedback/Toaster",
  component: Toaster,
};

export default meta;

type Story = StoryObj<typeof Toaster>;

export const Tipos: Story = {
  render: () => (
    <div className="flex flex-wrap gap-3">
      <Toaster />
      <Button onClick={() => notify.success("Credencial salva")}>Sucesso</Button>
      <Button variant="outline" onClick={() => notify.info("Novo dispositivo conectado", "169.254.1.1")}>
        Informação
      </Button>
      <Button variant="outline" onClick={() => notify.warning("Galaxy J4+ desconectado")}>
        Aviso
      </Button>
      <Button variant="destructive" onClick={() => notify.error("Não foi possível instalar o Nginx")}>
        Erro
      </Button>
    </div>
  ),
};
