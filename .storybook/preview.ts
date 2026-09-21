import type { Preview } from "@storybook/react-vite";
import "../src/index.css";

const preview: Preview = {
  parameters: {
    layout: "padded",
    backgrounds: { disable: true },
  },
};

export default preview;
