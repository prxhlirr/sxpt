import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const source = (path: string) =>
  readFileSync(resolve(process.cwd(), path), 'utf8');

describe('讲解与学习附件预览', () => {
  it('附件面板为教学点和节点附件提供预览与下载入口', () => {
    const panel = source('src/components/lesson/AttachmentPanel.vue');

    expect(panel).toContain('preview: [attachment: TrainingAttachment]');
    expect(panel).toContain("emit('preview', attachment)");
    expect(panel).toContain(':download="attachment.name"');
    expect(panel).toContain('预览');
  });

  it('共享播放组件统一管理菜单入口、最小化及最大化状态', () => {
    const playback = source(
      'src/components/lesson/LessonPlaybackPlayer.vue'
    );

    expect(playback).toContain('import AttachmentPreviewLayer');
    expect(playback).toContain(
      'const previewAttachment = ref<TrainingAttachment | null>(null)'
    );
    expect(playback).toContain(
      'const attachmentPreviewMinimized = ref(false)'
    );
    expect(playback).toContain(
      'const attachmentPreviewMaximized = ref(false)'
    );
    expect(playback).toContain('const currentAttachments = computed');
    expect(playback).toContain('@click="openAttachmentsFromMenu"');
    expect(playback).toContain('<small>附件</small>');
    expect(playback.match(/@preview="openAttachmentPreview"/g)).toHaveLength(3);
    expect(playback).toContain(':attachment="previewAttachment"');
    expect(playback).toContain(':attachments="currentAttachments"');
    expect(playback).toContain(':maximized="attachmentPreviewMaximized"');
    expect(playback).toContain('@maximize="maximizeAttachmentPreview"');
    expect(playback).toContain('@minimize="minimizeAttachmentPreview"');
    expect(playback).toContain('@restore="restoreAttachmentPreview"');
    expect(playback).toContain('@restore-size="restoreAttachmentPreviewSize"');
  });

  it('使用 Open File Viewer 统一预览 Office、PDF 和常用附件格式', () => {
    const preview = source(
      'src/components/lesson/AttachmentPreviewLayer.vue'
    );

    expect(preview).toContain("from '@open-file-viewer/core'");
    expect(preview).toContain("import '@open-file-viewer/core/style.css'");
    expect(preview).toContain('createViewer({');
    expect(preview).toContain('resolvePreviewFit(attachment)');
    expect(preview).toContain("return isPdfOrWord ? 'width' : 'contain'");
    expect(preview).toContain('officePlugin({ pdf: pdfOptions })');
    expect(preview).toContain('pdfPlugin(pdfOptions)');
    expect(preview).toContain('fallbackPlugin()');
    expect(preview).toContain('file: attachment.dataUrl');
    expect(preview).toContain('fileName: attachment.name');
    expect(preview).toContain('mimeType: attachment.mimeType');
    expect(preview).toContain('viewer?.destroy()');
    expect(preview).toContain('class="attachment-preview-layer"');
    expect(preview).toContain('class="attachment-preview-list"');
    expect(preview).toContain('class="attachment-preview-minimized"');
    expect(preview).toContain('最小化到附件图标');
    expect(preview).toContain('beginOverlayDrag');
    expect(preview).toContain('@pointermove="moveMinimizedDrag"');
    expect(preview).toContain('@click="restoreFromMinimized"');
    expect(preview).toContain('suppressMinimizedClick');
    expect(preview).toContain('最大化附件预览');
    expect(preview).toContain("emit('restoreSize')");
    expect(preview).toContain('VIEWER_RESIZE_SETTLE_MS');
    expect(preview).toContain('@transitionend="handlePreviewTransitionEnd"');
    expect(preview).toContain('width: calc(100vw - 32px)');
    expect(preview).toContain(':download="attachment.name"');
    expect(preview).not.toContain('v-html');
  });

  it('上传接口只保存原文件并交给 Open File Viewer 解析', () => {
    const packageJson = source('package.json');
    const models = source('src/domain/models.ts');
    const api = source('src/api/attachments.ts');
    const attachments = source('src/utils/trainingAttachments.ts');
    const preview = source(
      'src/components/lesson/AttachmentPreviewLayer.vue'
    );

    expect(packageJson).toContain('"@open-file-viewer/core": "0.1.32"');
    expect(packageJson).toContain('"pdfjs-dist": "4.10.38"');
    expect(models).toContain('downloadUrl?: string');
    expect(models).not.toContain('previewUrl?: string');
    expect(models).not.toContain('previewStatus?:');
    expect(api).toContain('export const teachingAttachmentApi');
    expect(api).toContain("formData.append('file', file, file.name)");
    expect(api).toContain("url: '/teaching-attachments'");
    expect(api).not.toContain('timeout: 70_000');
    expect(attachments).toContain('teachingAttachmentApi.upload(file)');
    expect(preview).toContain('props.attachment?.downloadUrl');
    expect(preview).not.toContain('previewUrl');
    expect(preview).not.toContain('previewMimeType');
  });
});
