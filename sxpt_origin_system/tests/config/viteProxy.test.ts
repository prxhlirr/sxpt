import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { loadConfigFromFile } from 'vite';

describe('Vite API proxy', () => {
  it('forwards same-origin /api requests to the local backend', async () => {
    const loaded = await loadConfigFromFile(
      { command: 'serve', mode: 'test' },
      path.resolve(process.cwd(), 'vite.config.ts')
    );
    const proxy = loaded?.config.server?.proxy as
      | Record<string, { target?: string; changeOrigin?: boolean }>
      | undefined;

    expect(proxy?.['/api']).toMatchObject({
      target: 'http://127.0.0.1:8080',
      changeOrigin: true
    });
  });
});
