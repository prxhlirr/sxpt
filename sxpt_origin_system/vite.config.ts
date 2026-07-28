import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import fs from 'node:fs';
import path from 'node:path';
import { injectRecorderScript } from './src/engine/htmlInjection';

const localBusinessRoot = path.resolve(__dirname, 'local-business');

function contentTypeFor(filePath: string) {
  if (filePath.endsWith('.html')) return 'text/html; charset=utf-8';
  if (filePath.endsWith('.css')) return 'text/css; charset=utf-8';
  if (filePath.endsWith('.js')) return 'application/javascript; charset=utf-8';
  if (filePath.endsWith('.svg')) return 'image/svg+xml';
  if (filePath.endsWith('.png')) return 'image/png';
  if (filePath.endsWith('.jpg') || filePath.endsWith('.jpeg')) return 'image/jpeg';
  return 'application/octet-stream';
}

function localBusinessProxyPlugin() {
  return {
    name: 'local-business-proxy',
    configureServer(server) {
      server.middlewares.use('/biz-local', (req, res) => {
        const rawUrl = decodeURIComponent((req.url ?? '/').split('?')[0]);
        const requestPath = rawUrl === '/' ? '/business-demo.html' : rawUrl;
        const filePath = path.resolve(localBusinessRoot, `.${requestPath}`);

        if (!filePath.startsWith(localBusinessRoot) || !fs.existsSync(filePath)) {
          res.statusCode = 404;
          res.end('Not found');
          return;
        }

        if (filePath.endsWith('.html')) {
          const html = fs.readFileSync(filePath, 'utf-8');
          res.setHeader('content-type', contentTypeFor(filePath));
          res.end(injectRecorderScript(html, '/training-recorder.js'));
          return;
        }

        res.setHeader('content-type', contentTypeFor(filePath));
        fs.createReadStream(filePath).pipe(res);
      });
    }
  };
}

export default defineConfig({
  plugins: [vue(), localBusinessProxyPlugin()],
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'node',
    globals: true
  }
});
