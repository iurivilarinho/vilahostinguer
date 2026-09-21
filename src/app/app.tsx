import { AppProvider } from "./providers/app-provider";
import { AppRouter } from "./routing/router";

export const App = () => (
  <AppProvider>
    <AppRouter />
  </AppProvider>
);
