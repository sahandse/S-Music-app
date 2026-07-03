import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// base './' so built assets resolve under file:// when packaged by Electron
export default defineConfig({
  plugins: [react()],
  base: './',
  build: {
    outDir: 'dist',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    strictPort: true
  }
})
