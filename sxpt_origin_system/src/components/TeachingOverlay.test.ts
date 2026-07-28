import { createSSRApp, h } from 'vue';
import { renderToString } from '@vue/server-renderer';
import { describe, expect, it } from 'vitest';
import TeachingOverlay from './TeachingOverlay.vue';
import type { RecordedStep } from '../types/domain';

const guideStep: RecordedStep = {
  id: 'guide-1',
  nodeId: 'node-1',
  kind: 'guide',
  order: 1,
  title: '查看示意图',
  actionType: 'click',
  selector: '',
  rect: { x: 0, y: 0, width: 0, height: 0 },
  text: '',
  url: '/business-demo.html',
  teachingText: '请根据示意图继续操作。',
  practiceHint: '',
  examGoal: '',
  completionMethod: 'manual',
  anchor: { mode: 'center' },
  guideImage: {
    src: 'data:image/webp;base64,AAAA',
    name: 'guide.webp',
    mimeType: 'image/webp',
    width: 640,
    height: 360,
    storageType: 'inline'
  }
};

describe('TeachingOverlay', () => {
  it('renders the guide image below the teaching text', async () => {
    const html = await renderToString(
      createSSRApp({
        render: () =>
          h(TeachingOverlay, {
            step: guideStep,
            visible: true,
            showHint: true
          })
      })
    );

    expect(html.indexOf('请根据示意图继续操作。')).toBeLessThan(
      html.indexOf('data:image/webp;base64,AAAA')
    );
    expect(html).toContain('class="guide-bubble__image"');
    expect(html).toContain('alt="查看示意图说明图片"');
    expect(html).toContain('style="width:1200px;"');
  });

  it('does not widen an action step that contains stale guide image data', async () => {
    const html = await renderToString(
      createSSRApp({
        render: () =>
          h(TeachingOverlay, {
            step: { ...guideStep, kind: 'action' },
            visible: true,
            showHint: true
          })
      })
    );

    expect(html).toContain('style="width:320px;"');
    expect(html).not.toContain('class="guide-bubble__image"');
  });

  it('renders the compact teacher image popup at the same 1200px target width', async () => {
    const html = await renderToString(
      createSSRApp({
        render: () =>
          h(TeachingOverlay, {
            step: guideStep,
            visible: true,
            showHint: true,
            compact: true
          })
      })
    );

    expect(html).toContain('class="compact guide-bubble"');
    expect(html).toContain('style="width:1200px;"');
  });
});
