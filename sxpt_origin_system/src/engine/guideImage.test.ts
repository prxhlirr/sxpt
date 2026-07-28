import { describe, expect, it, vi } from 'vitest';
import {
  GUIDE_IMAGE_MAX_DATA_URL_LENGTH,
  GUIDE_IMAGE_MAX_SOURCE_BYTES,
  prepareGuideImage,
  takeSelectedGuideImageFile
} from './guideImage';

const file = (overrides: Partial<File> = {}) =>
  ({
    name: 'guide.png',
    type: 'image/png',
    size: 1024,
    ...overrides
  }) as File;

describe('prepareGuideImage', () => {
  it('scales the longest edge and returns inline image metadata', async () => {
    const dispose = vi.fn();
    const encode = vi.fn(
      async (_source: CanvasImageSource, width: number, height: number) => {
        expect({ width, height }).toEqual({ width: 1280, height: 640 });
        return {
          src: 'data:image/webp;base64,AAAA',
          mimeType: 'image/webp'
        };
      }
    );

    const result = await prepareGuideImage(file(), {
      decode: async () => ({
        source: {} as CanvasImageSource,
        width: 2560,
        height: 1280,
        dispose
      }),
      encode
    });

    expect(result).toEqual({
      src: 'data:image/webp;base64,AAAA',
      name: 'guide.png',
      mimeType: 'image/webp',
      width: 1280,
      height: 640,
      storageType: 'inline'
    });
    expect(dispose).toHaveBeenCalledOnce();
  });

  it('rejects unsupported image formats before decoding', async () => {
    await expect(
      prepareGuideImage(file({ type: 'image/gif' }), {
        decode: vi.fn(),
        encode: vi.fn()
      })
    ).rejects.toThrow('仅支持 PNG、JPEG 或 WebP 图片');
  });

  it('rejects source files larger than 5MB', async () => {
    await expect(
      prepareGuideImage(file({ size: GUIDE_IMAGE_MAX_SOURCE_BYTES + 1 }), {
        decode: vi.fn(),
        encode: vi.fn()
      })
    ).rejects.toThrow('图片大小不能超过 5MB');
  });

  it('rejects an encoded Data URL beyond the demo limit and releases the bitmap', async () => {
    const dispose = vi.fn();

    await expect(
      prepareGuideImage(file(), {
        decode: async () => ({
          source: {} as CanvasImageSource,
          width: 800,
          height: 600,
          dispose
        }),
        encode: async () => ({
          src:
            'data:image/webp;base64,' +
            'A'.repeat(GUIDE_IMAGE_MAX_DATA_URL_LENGTH),
          mimeType: 'image/webp'
        })
      })
    ).rejects.toThrow('处理后的图片仍然过大');
    expect(dispose).toHaveBeenCalledOnce();
  });

  it('rejects browsers that fall back to a non-WebP canvas result', async () => {
    await expect(
      prepareGuideImage(file(), {
        decode: async () => ({
          source: {} as CanvasImageSource,
          width: 800,
          height: 600
        }),
        encode: async () => ({
          src: 'data:image/png;base64,AAAA',
          mimeType: 'image/webp'
        })
      })
    ).rejects.toThrow('当前浏览器无法生成 WebP 图片');
  });
});

describe('takeSelectedGuideImageFile', () => {
  it('returns the selected file and clears the input for selecting it again', () => {
    const selectedFile = file();
    const input = {
      files: [selectedFile],
      value: 'C:\\fakepath\\guide.png'
    };

    expect(takeSelectedGuideImageFile(input)).toBe(selectedFile);
    expect(input.value).toBe('');
  });
});
