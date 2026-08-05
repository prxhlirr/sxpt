import type { CaptureRect } from '../domain/models';

export interface ViewportSize {
  width: number;
  height: number;
}

export interface ViewportPlacement extends ViewportSize {
  left: number;
  top: number;
  scale: number;
}

export interface ViewportRect {
  left: number;
  top: number;
  width: number;
  height: number;
}

export const DEFAULT_RECORDING_VIEWPORT: ViewportSize = {
  width: 1366,
  height: 768
};

/**
 * 业务功能：规范化录制视口尺寸，避免历史数据缺失或非法数值导致回放布局失真。
 * 关键流程：只接受有限且大于 0 的宽高；不合法时返回 undefined，由调用方使用默认兜底。
 */
export function normalizeViewport(
  viewport?: Partial<ViewportSize>
): ViewportSize | undefined {
  const width = Number(viewport?.width);
  const height = Number(viewport?.height);
  if (!Number.isFinite(width) || !Number.isFinite(height)) return undefined;
  if (width <= 0 || height <= 0) return undefined;
  return {
    width,
    height
  };
}

/**
 * 业务功能：计算录制视口在容器内等比完整展示的位置。
 * 关键流程：取横纵缩放比的较小值，居中放置，保证录制页面不被裁剪。
 */
export function calculateContainedViewport(
  viewport: ViewportSize,
  container: ViewportSize
): ViewportPlacement {
  const safeViewport = normalizeViewport(viewport) ?? DEFAULT_RECORDING_VIEWPORT;
  const safeContainer = normalizeViewport(container) ?? safeViewport;
  const containerWidth = safeContainer.width;
  const containerHeight = safeContainer.height;
  const scale = Math.min(
    containerWidth / safeViewport.width,
    containerHeight / safeViewport.height
  );
  const width = safeViewport.width * scale;
  const height = safeViewport.height * scale;
  return {
    width: safeViewport.width,
    height: safeViewport.height,
    left: (containerWidth - width) / 2,
    top: (containerHeight - height) / 2,
    scale
  };
}

/**
 * 业务功能：把录制时的元素矩形映射到填充展示容器。
 * 关键流程：横纵轴分别按容器比例缩放，适用于 iframe 拉伸填满的场景。
 */
export function mapRectToFilledViewport(
  rect: CaptureRect,
  viewport: ViewportSize,
  container: ViewportSize
): ViewportRect {
  const safeViewport = normalizeViewport(viewport) ?? DEFAULT_RECORDING_VIEWPORT;
  const safeContainer = normalizeViewport(container) ?? safeViewport;
  const scaleX = safeContainer.width / safeViewport.width;
  const scaleY = safeContainer.height / safeViewport.height;
  return {
    left: rect.x * scaleX,
    top: rect.y * scaleY,
    width: rect.width * scaleX,
    height: rect.height * scaleY
  };
}

/**
 * 业务功能：把录制时的元素矩形映射到等比完整展示容器。
 * 关键流程：复用 contained 视口偏移与统一缩放比例，保证高亮位置与页面一致。
 */
export function mapRectToContainedViewport(
  rect: CaptureRect,
  viewport: ViewportSize,
  container: ViewportSize
): ViewportRect {
  const placement = calculateContainedViewport(viewport, container);
  return {
    left: placement.left + rect.x * placement.scale,
    top: placement.top + rect.y * placement.scale,
    width: rect.width * placement.scale,
    height: rect.height * placement.scale
  };
}
