import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Vite configuration.
 *
 * The dev-server proxy is the important part: it forwards /api and the root-level
 * redirect to the Spring Boot backend on :8080, so during development the browser
 * only ever talks to one origin and CORS never enters the picture. The backend's
 * CORS config still matters in production, where the two are genuinely on
 * different hosts.
 */
export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,

        /**
         * Strip the browser's Origin header before forwarding.
         *
         * `changeOrigin` rewrites Host but leaves Origin alone, so the backend
         * still sees `http://localhost:5173` and evaluates CORS on a request
         * that — after proxying — is not cross-origin at all. That works only
         * while Vite holds the exact port named in CORS_ALLOWED_ORIGINS. The
         * moment 5173 is already taken, Vite silently falls back to 5174 and
         * every API call fails with `403 Invalid CORS request`, including login
         * and registration. The dev server starts fine and the app renders, so
         * it looks like the backend is broken rather than like a port clash.
         *
         * Removing the header makes the backend treat it as an ordinary
         * same-origin request, which is what it actually is. Production is
         * unaffected: there is no proxy there, and the real CORS config still
         * applies.
         */
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin');
          });
        },
      },
    },
  },

  build: {
    outDir: 'dist',
    sourcemap: true,

    /**
     * Explicit browser floor rather than Vite's default 'modules'.
     *
     * The default targets whatever the current esbuild considers baseline, which
     * drifts between releases and has shipped syntax that older Safari chokes on.
     * Pinning it means the output is verifiably parseable by these versions, and
     * the build fails loudly rather than a Safari user getting a blank page.
     * Safari 15.4 is the floor because that is where `scroll-behavior: smooth`
     * landed, which this UI relies on.
     */
    target: ['es2020', 'chrome90', 'edge90', 'firefox90', 'safari15.4'],
    rollupOptions: {
      output: {
        // Split React out of the app bundle. It changes far less often than
        // application code, so browsers keep it cached across deploys.
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
        },
      },
    },
  },
});
