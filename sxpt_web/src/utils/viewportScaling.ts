export interface ViewportSize {
  width: number;
  height: number;
}

export interface ContainedViewport extends ViewportSize {
  left: number;
  top: number;
  scale: number;
}

export interface ViewportRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export const DEFAULT_RECORDING_VIEWPORT: ViewportSize = {
  width: 1440,
  height: 900
};

const MIN_VIEWPORT_EDGE = 240;
const MAX_VIEWPORT_EDGE = 7680;

export function normalizeViewport(
  viewport?: Partial<ViewportSize>,
  fallback?: ViewportSize
): ViewportSize | undefined {
  const width = Number(viewport?.width);
  const height = Number(viewport?.height);
  if (
    Number.isFinite(width) &&
    Number.isFinite(height) &&
    width >= MIN_VIEWPORT_EDGE &&
    height >= MIN_VIEWPORT_EDGE &&
    width <= MAX_VIEWPORT_EDGE &&
    height <= MAX_VIEWPORT_EDGE
  ) {
    return {
      width: Math.round(width),
      height: Math.round(height)
    };
  }
  return fallback ? { ...fallback } : undefined;
}

export function calculateContainedViewport(
  viewport: ViewportSize,
  container: ViewportSize
): ContainedViewport {
  const safeViewport = normalizeViewport(viewport, DEFAULT_RECORDING_VIEWPORT)!;
  const containerWidth = Math.max(0, Number(container.width) || 0);
  const containerHeight = Math.max(0, Number(container.height) || 0);
  const scale =
    containerWidth > 0 && containerHeight > 0
      ? Math.min(
          containerWidth / safeViewport.width,
          containerHeight / safeViewport.height
        )
      : 1;
  const width = safeViewport.width * scale;
  const height = safeViewport.height * scale;
  return {
    width,
    height,
    scale,
    left: (containerWidth - width) / 2,
    top: (containerHeight - height) / 2
  };
}

export function mapRectToContainedViewport(
  rect: ViewportRect,
  viewport: ViewportSize,
  container: ViewportSize
) {
  const safeViewport = normalizeViewport(viewport, DEFAULT_RECORDING_VIEWPORT)!;
  const placement = calculateContainedViewport(safeViewport, container);
  const x = Math.min(safeViewport.width, Math.max(0, Number(rect.x) || 0));
  const y = Math.min(safeViewport.height, Math.max(0, Number(rect.y) || 0));
  const width = Math.min(
    safeViewport.width - x,
    Math.max(0, Number(rect.width) || 0)
  );
  const height = Math.min(
    safeViewport.height - y,
    Math.max(0, Number(rect.height) || 0)
  );
  return {
    left: placement.left + x * placement.scale,
    top: placement.top + y * placement.scale,
    width: width * placement.scale,
    height: height * placement.scale
  };
}

export function mapRectToFilledViewport(
  rect: ViewportRect,
  viewport: ViewportSize,
  container: ViewportSize
) {
  const safeViewport = normalizeViewport(viewport, DEFAULT_RECORDING_VIEWPORT)!;
  const containerWidth = Math.max(0, Number(container.width) || 0);
  const containerHeight = Math.max(0, Number(container.height) || 0);
  const scaleX = containerWidth > 0 ? containerWidth / safeViewport.width : 1;
  const scaleY = containerHeight > 0 ? containerHeight / safeViewport.height : 1;
  const x = Math.min(safeViewport.width, Math.max(0, Number(rect.x) || 0));
  const y = Math.min(safeViewport.height, Math.max(0, Number(rect.y) || 0));
  const width = Math.min(
    safeViewport.width - x,
    Math.max(0, Number(rect.width) || 0)
  );
  const height = Math.min(
    safeViewport.height - y,
    Math.max(0, Number(rect.height) || 0)
  );
  return {
    left: x * scaleX,
    top: y * scaleY,
    width: width * scaleX,
    height: height * scaleY
  };
}
