import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
//Configuración de la herramienta de construcción Vite para habilitar soporte React
export default defineConfig({
  plugins: [react()],
})
