import type { TrainingAttachment } from '../domain/models';

export const MAX_TRAINING_ATTACHMENT_BYTES = 5 * 1024 * 1024;

export function formatAttachmentSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function readFileAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ''));
    reader.onerror = () => reject(new Error(`读取附件“${file.name}”失败。`));
    reader.readAsDataURL(file);
  });
}

export async function createTrainingAttachments(
  files: FileList | File[]
): Promise<TrainingAttachment[]> {
  const source = Array.from(files);
  const oversized = source.find(
    (file) => file.size > MAX_TRAINING_ATTACHMENT_BYTES
  );
  if (oversized) {
    throw new Error(`附件“${oversized.name}”超过 5 MB，无法上传。`);
  }

  return Promise.all(
    source.map(async (file, index) => ({
      id: `attachment-${Date.now()}-${index}-${Math.random()
        .toString(36)
        .slice(2, 8)}`,
      name: file.name,
      mimeType: file.type || 'application/octet-stream',
      size: file.size,
      dataUrl: await readFileAsDataUrl(file),
      uploadedAt: new Date().toISOString()
    }))
  );
}
