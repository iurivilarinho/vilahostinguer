/// <reference types="vitest/config" />
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import path from "node:path";
import { defineConfig } from "vite";

const backend = "http://127.0.0.1:8747";

// O build vai direto para o backend: o Spring serve a interface de dentro do jar.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { "@": path.resolve(__dirname, "src") },
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": { target: backend, changeOrigin: true },
      "/ws": { target: backend.replace("http", "ws"), ws: true, changeOrigin: true },
    },
  },
  build: {
    outDir: "backend/src/main/resources/static",
    emptyOutDir: true,
    chunkSizeWarningLimit: 1500,
    // duas páginas: o painel administrativo (index.html) e o painel do cliente (portal.html)
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, "index.html"),
        portal: path.resolve(__dirname, "portal.html"),
      },
    },
  },
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
