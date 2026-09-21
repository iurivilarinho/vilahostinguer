import type { Meta, StoryObj } from "@storybook/react-vite";
import { Skeleton } from "./index";

const meta: Meta<typeof Skeleton> = {
  title: "Feedback/Skeleton",
  component: Skeleton,
};

export default meta;

type Story = StoryObj<typeof Skeleton>;

export const Cartao: Story = {
  render: () => (
    <div className="flex max-w-sm flex-col gap-3">
      <Skeleton className="h-6 w-40" />
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-2/3" />
    </div>
  ),
};
