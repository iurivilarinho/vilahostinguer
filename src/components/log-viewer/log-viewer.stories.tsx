import type { Meta, StoryObj } from "@storybook/react-vite";
import { LogViewer } from "./index";

const meta: Meta<typeof LogViewer> = {
  title: "Dados/LogViewer",
  component: LogViewer,
  args: {
    text: "== Instalando Nginx (nginx) ==\nfetch https://dl-cdn.alpinelinux.org/alpine/edge/main/aarch64/APKINDEX.tar.gz\n(1/2) Installing pcre2 (10.44-r0)\n(2/2) Installing nginx (1.26.2-r0)\nOK: 412 MiB in 180 packages\n== Concluído ==",
  },
};

export default meta;

type Story = StoryObj<typeof LogViewer>;

export const Padrao: Story = {};
