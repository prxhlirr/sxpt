import { createSSRApp } from 'vue';
import { renderToString, type SSRContext } from '@vue/server-renderer';
import { describe, expect, it } from 'vitest';
import type { TrainingAttachment } from '../../domain/models';
import AttachmentPreviewLayer from './AttachmentPreviewLayer.vue';

const attachment = (id: string): TrainingAttachment => ({
  id,
  name: `${id}.pdf`,
  mimeType: 'application/pdf',
  size: 1024,
  dataUrl: `data:application/pdf;base64,${id}`,
  uploadedAt: '2026-08-07T00:00:00.000Z'
});

async function renderLayer(
  props: Record<string, unknown>
): Promise<string> {
  const context: SSRContext = {};
  await renderToString(createSSRApp(AttachmentPreviewLayer, props), context);
  return Object.values(context.teleports ?? {}).join('');
}

describe('AttachmentPreviewLayer preview states', () => {
  it('does not float attachments before the user opens one from the menu', async () => {
    const html = await renderLayer({
      attachments: [attachment('guide'), attachment('example')],
      contextKey: 'stage-a'
    });

    expect(html).not.toContain('attachment-preview-layer');
    expect(html).not.toContain('attachment-preview-minimized');
  });

  it('opens a side preview with attachment switching and maximize controls', async () => {
    const html = await renderLayer({
      attachment: attachment('guide'),
      attachments: [attachment('guide'), attachment('example')],
      contextKey: 'stage-a',
      minimized: false
    });

    expect(html).toContain('attachment-preview-layer');
    expect(html).toContain('attachment-preview-list');
    expect(html).toContain('guide.pdf');
    expect(html).toContain('example.pdf');
    expect(html).toContain('最大化附件预览');
    expect(html).not.toContain('attachment-preview-minimized');
  });

  it('reduces the preview to one attachment icon when minimized', async () => {
    const html = await renderLayer({
      attachment: attachment('guide'),
      attachments: [attachment('guide'), attachment('example')],
      contextKey: 'stage-a',
      minimized: true
    });

    expect(html).toContain('attachment-preview-minimized');
    expect(html).not.toContain('attachment-preview-window');
    expect(html).not.toContain('attachment-preview-list');
  });

  it('renders the full-viewport state with a restore control', async () => {
    const html = await renderLayer({
      attachment: attachment('guide'),
      attachments: [attachment('guide')],
      contextKey: 'stage-a',
      maximized: true
    });

    expect(html).toContain('attachment-preview-layer');
    expect(html).toContain('maximized');
    expect(html).toContain('aria-modal="true"');
    expect(html).toContain('还原附件预览');
    expect(html).not.toContain('最大化附件预览');
  });
});
