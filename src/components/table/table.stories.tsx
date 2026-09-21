import type { Meta, StoryObj } from "@storybook/react-vite";
import { Badge } from "../badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "./index";

const meta: Meta<typeof Table> = {
  title: "Dados/Table",
  component: Table,
};

export default meta;

type Story = StoryObj<typeof Table>;

export const Padrao: Story = {
  render: () => (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Operação</TableHead>
          <TableHead>Dispositivo</TableHead>
          <TableHead>Situação</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow>
          <TableCell>Instalar Nginx</TableCell>
          <TableCell>Galaxy J4+</TableCell>
          <TableCell>
            <Badge tone="success">Concluída</Badge>
          </TableCell>
        </TableRow>
        <TableRow>
          <TableCell>Backup: /etc</TableCell>
          <TableCell>Raspberry Pi</TableCell>
          <TableCell>
            <Badge tone="info">Em execução</Badge>
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>
  ),
};
