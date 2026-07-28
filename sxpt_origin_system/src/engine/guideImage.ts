import type { GuideImage } from '../types/domain';

export const GUIDE_IMAGE_ACCEPT = 'image/png,image/jpeg,image/webp';
export const GUIDE_IMAGE_MAX_SOURCE_BYTES = 5 * 1024 * 1024;
export const GUIDE_IMAGE_MAX_DATA_URL_LENGTH = 600 * 1024;
export const GUIDE_IMAGE_MAX_EDGE = 1280;

const acceptedTypes = new Set(GUIDE_IMAGE_ACCEPT.split(','));

export interface DecodedGuideImage {
  source: CanvasImageSource;
  width: number;
  height: number;
  dispose?: () => void;
}

export interface EncodedGuideImage {
  src: string;
  mimeType: string;
}

export interface GuideImageDependencies {
  decode: (file: File) => Promise<DecodedGuideImage>;
  encode: (
    source: CanvasImageSource,
    width: number,
    height: number
  ) => Promise<EncodedGuideImage>;
}

export interface GuideImageFileInput {
  files: ArrayLike<File> | null;
  value: string;
}

export function takeSelectedGuideImageFile(
  input: GuideImageFileInput
): File | undefined {
  const selectedFile = input.files?.[0];
  input.value = '';
  return selectedFile;
}

export async function prepareGuideImage(
  file: File,
  dependencies: GuideImageDependencies = browserDependencies
): Promise<GuideImage> {
  if (!acceptedTypes.has(file.type)) {
    throw new Error('仅支持 PNG、JPEG 或 WebP 图片。');
  }
  if (file.size <= 0) {
    throw new Error('图片文件不能为空。');
  }
  if (file.size > GUIDE_IMAGE_MAX_SOURCE_BYTES) {
    throw new Error('图片大小不能超过 5MB。');
  }

  let decoded: DecodedGuideImage;
  try {
    decoded = await dependencies.decode(file);
  } catch {
    throw new Error('无法读取图片，请重新选择有效的图片文件。');
  }

  try {
    if (decoded.width <= 0 || decoded.height <= 0) {
      throw new Error('无法读取图片，请重新选择有效的图片文件。');
    }

    const scale = Math.min(
      1,
      GUIDE_IMAGE_MAX_EDGE / Math.max(decoded.width, decoded.height)
    );
    const width = Math.max(1, Math.round(decoded.width * scale));
    const height = Math.max(1, Math.round(decoded.height * scale));
    const encoded = await dependencies.encode(decoded.source, width, height);

    if (
      encoded.mimeType !== 'image/webp' ||
      !/^data:image\/webp(?:;|,)/.test(encoded.src)
    ) {
      throw new Error('当前浏览器无法生成 WebP 图片，请更换浏览器后重试。');
    }

    if (encoded.src.length > GUIDE_IMAGE_MAX_DATA_URL_LENGTH) {
      throw new Error(
        '处理后的图片仍然过大，请选择内容更简单或尺寸更小的图片。'
      );
    }

    return {
      src: encoded.src,
      name: file.name,
      mimeType: encoded.mimeType,
      width,
      height,
      storageType: 'inline'
    };
  } finally {
    decoded.dispose?.();
  }
}

const browserDependencies: GuideImageDependencies = {
  async decode(file) {
    const bitmap = await createImageBitmap(file);
    return {
      source: bitmap,
      width: bitmap.width,
      height: bitmap.height,
      dispose: () => bitmap.close()
    };
  },
  async encode(source, width, height) {
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('当前浏览器无法处理图片。');
    }
    context.drawImage(source, 0, 0, width, height);
    return {
      src: canvas.toDataURL('image/webp', 0.82),
      mimeType: 'image/webp'
    };
  }
};
