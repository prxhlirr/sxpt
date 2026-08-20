import { describe, expect, it } from 'vitest';
import type { UserConfig } from 'vite';
import viteConfig from '../vite.config';

describe('Vite dependency optimization', () => {
  it('prebundles jszip while loading open-file-viewer directly', () => {
    const config = viteConfig as UserConfig;

    expect(config.optimizeDeps?.exclude).toContain('@open-file-viewer/core');
    expect(config.optimizeDeps?.include).toContain('jszip');
  });
});
