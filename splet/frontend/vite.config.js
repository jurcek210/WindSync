import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
<<<<<<< HEAD
=======
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:3001", 
        // target: "http://backend:3001", //za docker na virtualki
        changeOrigin: true,
      },
    },
  }
>>>>>>> dev
})
