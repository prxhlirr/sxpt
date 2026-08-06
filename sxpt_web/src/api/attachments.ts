import type { TrainingAttachment } from '../domain/models';
import { apiRequest } from './http';

/**
 * 教学附件上传接口。
 *
 * FormData 交给 Axios/浏览器自动生成 multipart boundary；后端只保存原文件，
 * 预览由 Open File Viewer 在浏览器内完成。
 */
export const teachingAttachmentApi = {
  upload(file: File) {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return apiRequest<TrainingAttachment>({
      method: 'POST',
      url: '/teaching-attachments',
      data: formData
    });
  }
};
