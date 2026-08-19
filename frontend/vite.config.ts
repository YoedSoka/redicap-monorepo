import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true, // acepta conexiones de la red local (ej. desde el celular), no solo localhost
    // Vite valida el header Host y rechaza dominios desconocidos desde 5.4.12. Sin esto,
    // todo lo que entre por el túnel de pruebas (ngrok) recibe "Blocked request. This host
    // is not allowed." — parece que ngrok no funciona, pero es Vite defendiéndose.
    // ngrok-free.app = subdominios efímeros clásicos; ngrok-free.dev = dominio estático
    // gratuito reservado de cuenta (el que realmente asigna hoy por default).
    allowedHosts: ['.ngrok-free.app', '.ngrok-free.dev'],
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
