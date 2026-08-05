import { describe, expect, it } from 'vitest';
import {
  normalizeBusinessPageSnapshot,
  normalizeSnapshotStyleUrls,
  rebaseSnapshotCssUrls,
  resolveSnapshotResourceUrl
} from './businessSnapshot';

describe('business snapshot styles', () => {
  it('resolves stylesheet and asset paths against their original location', () => {
    expect(
      resolveSnapshotResourceUrl(
        '../theme/main.css',
        'https://business.example.com/assets/css/app.css'
      )
    ).toBe('https://business.example.com/assets/theme/main.css');

    const css = rebaseSnapshotCssUrls(
      `@import "./theme.css";
       .logo { background: url('../images/logo.png'); }
       .font { src: url(./fonts/main.woff2) format('woff2'); }
       .inline { background: url(data:image/png;base64,abc); }`,
      'https://business.example.com/assets/css/app.css'
    );

    expect(css).toContain(
      '@import "https://business.example.com/assets/css/theme.css"'
    );
    expect(css).toContain(
      "url('https://business.example.com/assets/images/logo.png')"
    );
    expect(css).toContain(
      'url(https://business.example.com/assets/css/fonts/main.woff2)'
    );
    expect(css).toContain('url(data:image/png;base64,abc)');

    expect(
      rebaseSnapshotCssUrls(
        '.cdn { background: url(//cdn.example.com/icon.svg); }',
        'https://business.example.com/app/'
      )
    ).toContain('url(https://cdn.example.com/icon.svg)');
  });

  it('keeps only safe unique stylesheet URLs', () => {
    expect(
      normalizeSnapshotStyleUrls(
        [
          '/assets/app.css',
          '/assets/app.css',
          '../theme.css',
          'javascript:alert(1)',
          'data:text/css,body{}'
        ],
        'https://business.example.com/modules/order/page'
      )
    ).toEqual([
      'https://business.example.com/assets/app.css',
      'https://business.example.com/modules/theme.css'
    ]);
  });

  it('normalizes the new style metadata while accepting existing snapshots', () => {
    const snapshot = normalizeBusinessPageSnapshot(
      {
        version: 1,
        format: 'DOM',
        pageUrl: 'https://business.example.com/order/edit',
        pageTitle: '订单编辑',
        capturedAt: '2026-08-04T08:00:00.000Z',
        html: '<body class="theme-dark"><main>订单</main></body>',
        cssText: 'main { color: red; }',
        styleUrls: ['/assets/app.css']
      },
      { pageUrl: '/', pageTitle: '业务系统' }
    );

    expect(snapshot?.styleUrls).toEqual([
      'https://business.example.com/assets/app.css'
    ]);
    expect(snapshot?.html).toContain('theme-dark');
  });

  it('resolves legacy relative snapshot routes against the business platform', () => {
    const snapshot = normalizeBusinessPageSnapshot(
      {
        version: 1,
        format: 'DOM',
        pageUrl: '/oa/orders/edit',
        pageTitle: '订单编辑',
        capturedAt: '2026-08-04T08:00:00.000Z',
        html: '<body><img src="/assets/logo.png"></body>',
        styleUrls: ['/assets/app.css']
      },
      {
        pageUrl: 'https://oa.example.com/platform/',
        pageTitle: 'OA 系统'
      }
    );

    expect(snapshot?.pageUrl).toBe('https://oa.example.com/oa/orders/edit');
    expect(snapshot?.styleUrls).toEqual(['https://oa.example.com/assets/app.css']);
  });

  it('keeps Vue scoped component styles after a large UI library stylesheet', () => {
    const dependencyCss = '.el-control{color:#333}'.repeat(18_000);
    const componentCss =
      '.login-page[data-v-08aaad4e]{display:flex;width:100vw;height:100vh}';
    const cssText = `${dependencyCss}\n${componentCss}`;
    expect(cssText.length).toBeGreaterThan(400_000);

    const snapshot = normalizeBusinessPageSnapshot(
      {
        version: 1,
        format: 'DOM',
        pageUrl: 'https://business.example.com/login',
        pageTitle: '登录',
        capturedAt: '2026-08-04T08:00:00.000Z',
        html: '<body><div class="login-page" data-v-08aaad4e></div></body>',
        cssText
      },
      { pageUrl: '/', pageTitle: '业务系统' }
    );

    expect(snapshot?.cssText).toHaveLength(cssText.length);
    expect(snapshot?.cssText).toContain(componentCss);
  });
});
