import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

describe('public training runtime config', () => {
  it('selects the remote API through the same-origin proxy', () => {
    const script = readFileSync(
      new URL('../../public/training-config.js', import.meta.url),
      'utf8'
    );

    expect(script).toContain("apiMode: 'remote'");
    expect(script).toContain("apiBaseUrl: '/api/v1'");
    expect(script).toContain("bearerToken: ''");
  });

  it('reports hard navigation and SPA route changes to the parent frame', () => {
    const script = readFileSync(
      new URL('../../public/training-recorder.js', import.meta.url),
      'utf8'
    );

    expect(script).toContain("type: 'BUSINESS_NAVIGATING'");
    expect(script).toContain("addEventListener('pagehide'");
    expect(script).toContain("wrapHistoryMethod('pushState')");
    expect(script).toContain("wrapHistoryMethod('replaceState')");
    expect(script).toContain("addEventListener('popstate'");
    expect(script).toContain("addEventListener('hashchange'");
    expect(script).toContain('window.location.hash');
    expect(script).toContain('event.source !== window.parent');
  });
});
