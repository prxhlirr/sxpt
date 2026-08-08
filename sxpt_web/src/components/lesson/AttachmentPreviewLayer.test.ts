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

describe('AttachmentPreviewLayer teaching-point dock', () => {
  it('renders every teaching-point attachment as an individual stacked card', async () => {
    const html = await renderLayer({
      attachments: [attachment('guide'), attachment('example')],
      contextKey: 'stage-a'
    });

    expect(html.match(/attachment-preview-dock-card/g)).toHaveLength(2);
    expect(html).toContain('guide.pdf');
    expect(html).toContain('example.pdf');
    expect(html).toContain('拖动全部附件');
  });

  it('shows the full preview instead of dock cards while an attachment is open', async () => {
    const html = await renderLayer({
      attachment: attachment('guide'),
      attachments: [attachment('guide'), attachment('example')],
      contextKey: 'stage-a',
      minimized: false
    });

    expect(html).toContain('attachment-preview-modal');
    expect(html).not.toContain('attachment-preview-dock-card');
  });
});
