import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const backend = env.VITE_API_URL || 'http://localhost:8080/api'
  const proxy = {
    // Mantém as chamadas e o cookie de sessão na mesma origem do frontend.
    '/api': { target: new URL(backend).origin, changeOrigin: true },
  }

  return {
    plugins: [react()],
    resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
    server: { proxy },
    preview: { proxy },
  }
})
