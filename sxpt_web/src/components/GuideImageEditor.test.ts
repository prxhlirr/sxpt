import { createSSRApp, h } from 'vue';
import { renderToString } from '@vue/server-renderer';
import { describe, expect, it } from 'vitest';
import GuideImageEditor from './GuideImageEditor.vue';

async function render(props = {}) {
  return renderToString(
    createSSRApp({
      render: () => h(GuideImageEditor, props)
    })
  );
}

describe('GuideImageEditor', () => {
  it('offers a single image picker with the supported formats', async () => {
    const html = await render();

    expect(html).toContain('说明图片');
    expect(html).toContain('type="file"');
    expect(html).toContain('accept="image/png,image/jpeg,image/webp"');
    expect(html).toContain('选择图片');
  });

  it('renders the current image with replace and remove controls', async () => {
    const html = await render({
      image: {
        src: 'data:image/webp;base64,AAAA',
        name: 'guide.webp',
        mimeType: 'image/webp',
        width: 640,
        height: 360,
        storageType: 'inline'
      }
    });

    expect(html).toContain('src="data:image/webp;base64,AAAA"');
    expect(html).toContain('guide.webp');
    expect(html).toContain('替换图片');
    expect(html).toContain('删除图片');
  });

  it('disables the picker while an image is processing', async () => {
    const html = await render({ busy: true });

    expect(html).toContain('disabled');
  });
});
