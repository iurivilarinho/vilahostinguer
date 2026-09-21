import type { Meta, StoryObj } from "@storybook/react-vite";
import { FileUI } from "./index";

const meta: Meta = {
  title: "Formulário/FileUI",
};

export default meta;

type Story = StoryObj;

export const Input: Story = {
  render: () => <FileUI.Input onFilesSelected={(files) => console.info(files.map((file) => file.name))} multiple />,
};
