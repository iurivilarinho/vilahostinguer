import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { PortalApp } from "@/app/portal/portal-app";
import "@/index.css";

const root = document.getElementById("root");
if (root) {
  createRoot(root).render(
    <StrictMode>
      <PortalApp />
    </StrictMode>,
  );
}
