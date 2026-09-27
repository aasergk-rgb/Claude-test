import react from '@vitejs/plugin-react';
import { readFileSync } from 'node:fs';
import { defineConfig } from 'vite';

const { version } = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8'));

export default defineConfig({
  // 相対パスで出力し、どのサブパスに置いても動くようにする（GitHub Pages など）
  base: './',
  plugins: [react()],
  define: { __APP_VERSION__: JSON.stringify(version) },
});
