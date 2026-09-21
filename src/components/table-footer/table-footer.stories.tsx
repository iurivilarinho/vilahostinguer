import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import type { TablePagination } from "@/lib/api/types";
import { TableFooter } from "./index";

const meta: Meta<typeof TableFooter> = {
  title: "Dados/TableFooter",
  component: TableFooter,
};

export default meta;

type Story = StoryObj<typeof TableFooter>;

const Exemplo = () => {
  const [pagination, setPagination] = useState<TablePagination>({ page: 0, size: 10, totalElements: 42, totalPages: 5 });
  return (
    <TableFooter
      pagination={pagination}
      onPageChange={(page) => setPagination((current) => ({ ...current, page }))}
      onSizeChange={(size) => setPagination((current) => ({ ...current, size, page: 0 }))}
    />
  );
};

export const Padrao: Story = { render: () => <Exemplo /> };
