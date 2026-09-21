import type { Meta, StoryObj } from "@storybook/react-vite";
import { QueryErrorState } from "./index";

const meta: Meta<typeof QueryErrorState> = {
  title: "Feedback/QueryErrorState",
  component: QueryErrorState,
  args: { message: "O dispositivo não respondeu. Confira o cabo e tente de novo.", onRetry: () => undefined },
};

export default meta;

type Story = StoryObj<typeof QueryErrorState>;

export const Padrao: Story = {};
