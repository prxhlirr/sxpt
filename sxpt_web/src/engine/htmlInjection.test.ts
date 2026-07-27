import { describe, expect, it } from 'vitest';
import { injectRecorderScript } from './htmlInjection';

describe('htmlInjection', () => {
  it('injects the recorder script before the closing body tag', () => {
    const html = '<html><body><main>业务页面</main></body></html>';

    expect(injectRecorderScript(html, '/training-recorder.js')).toBe(
      '<html><body><main>业务页面</main><script src="/training-recorder.js"></script></body></html>'
    );
  });

  it('appends the recorder script when the page has no closing body tag', () => {
    const html = '<html><main>业务页面</main></html>';

    expect(injectRecorderScript(html, '/training-recorder.js')).toBe(
      '<html><main>业务页面</main></html><script src="/training-recorder.js"></script>'
    );
  });
});
