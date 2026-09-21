import type { Meta, StoryObj } from "@storybook/react-vite";
import { Button } from "../button";
import { Typography } from "../typography";
import { Card, CardContent, CardFooter, CardHeader } from "./index";

const meta: Meta<typeof Card> = {
  title: "Base/Card",
  component: Card,
};

export default meta;

type Story = StoryObj<typeof Card>;

export const Completo: Story = {
  render: () => (
    <Card className="max-w-md">
      <CardHeader>
        <div>
          <Typography variant="title-md">Recursos</Typography>
          <Typography variant="body-sm" className="text-muted-foreground">
            Uso nos últimos minutos
          </Typography>
        </div>
      </CardHeader>
      <CardContent>
        <Typography variant="body-md">Conteúdo do cartão.</Typography>
      </CardContent>
      <CardFooter>
        <Button variant="outline">Cancelar</Button>
        <Button>Salvar</Button>
      </CardFooter>
    </Card>
  ),
};
