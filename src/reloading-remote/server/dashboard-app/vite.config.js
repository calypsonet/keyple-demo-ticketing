import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Keyple Reload Demo server URL, used by the development server
const serverUrl = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    // Forwards the API calls to the Keyple Reload Demo server
    proxy: {
      '/activity': serverUrl,
      '/card': serverUrl,
    },
  },
  build: {
    // Output directory embedded into the server by the Gradle build
    outDir: 'build',
  },
});
