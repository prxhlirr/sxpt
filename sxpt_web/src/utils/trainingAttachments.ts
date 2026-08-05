import type { TrainingAttachment } from '../domain/models';

const MAX_ATTACHMENT_SIZE = 5 * 1024 * 1024;

/**
 * 业务功能：把教师上传的教学附件转为可随教案一起保存的内联数据。
 * 关键流程：逐个校验大小并读取为 dataUrl，避免演示环境额外依赖对象存储服务。
 */
export async function createTrainingAttachments(
  files: FileList
): Promise<TrainingAttachment[]> {
  const selectedFiles = Array.from(files);
  return Promise.all(selectedFiles.map(createTrainingAttachment));
}

/**
 * 业务功能：格式化附件大小，供编辑页和学习页使用同一套展示口径。
 * 关键流程：小文件显示 KB，大文件显示 MB，减少界面中的原始字节噪声。
 */
export function formatAttachmentSize(size: number): string {
  const normalized = Number.isFinite(size) && size > 0 ? size : 0;
  if (normalized >= 1024 * 1024) {
    return `${(normalized / 1024 / 1024).toFixed(1)} MB`;
  }
  return `${Math.max(1, Math.ceil(normalized / 1024))} KB`;
}

async function createTrainingAttachment(file: File): Promise<TrainingAttachment> {
  if (file.size > MAX_ATTACHMENT_SIZE) {
    throw new Error(`附件“${file.name}”超过 5 MB，请压缩后再上传`);
  }
  return {
    id: createAttachmentId(),
    name: file.name,
    mimeType: file.type || 'application/octet-stream',
    size: file.size,
    dataUrl: await readFileAsDataUrl(file),
    uploadedAt: new Date().toISOString()
  };
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.addEventListener('load', () => resolve(String(reader.result ?? '')));
    reader.addEventListener('error', () => reject(new Error(`附件“${file.name}”读取失败`)));
    reader.readAsDataURL(file);
  });
}

function createAttachmentId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `attachment-${crypto.randomUUID()}`;
  }
  return `attachment-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
