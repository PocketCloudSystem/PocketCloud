import { defineConfig } from 'vite';

// dev: `npm run dev` proxies the API to a local cloud (PANEL_TARGET, default http://localhost:8000)
const target = process.env.PANEL_TARGET || 'http://localhost:8000';

export default defineConfig({
  base: './',
  esbuild: { jsx: 'automatic', jsxImportSource: 'preact' },
  // maven sets PANEL_OUT_DIR to target/.../classes/panel so the files end up in the jar
  build: { outDir: process.env.PANEL_OUT_DIR || 'dist', emptyOutDir: true, assetsDir: 'assets' },
  server: { proxy: { '^/cloud/(?!panel)': { target, changeOrigin: true } } },
});
