import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // In development the React app runs on :5173 and the API on :8080.
    // The proxy forwards /api calls, so the browser sees one origin and no CORS setup is needed.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
