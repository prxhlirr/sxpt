import type { Rect } from '../types/domain';

export interface ViewportSize {
  width: number;
  height: number;
}

export interface BubbleSize {
  width: number;
  height: number;
}

export interface GuideBubblePlacement {
  left: number;
  top: number;
  placement: 'top' | 'bottom';
}

const margin = 12;

export function getGuideBubbleSize(
  compact: boolean,
  hasImage: boolean
): BubbleSize {
  if (hasImage) {
    return { width: 1200, height: compact ? 780 : 820 };
  }
  return compact
    ? { width: 280, height: 132 }
    : { width: 320, height: 178 };
}

export function getGuideBubblePlacement(
  rect: Rect,
  viewport: ViewportSize,
  bubble: BubbleSize
): GuideBubblePlacement {
  const preferredTop = rect.y - bubble.height - margin;
  const placement =
    bubble.height > viewport.height - margin * 2 || preferredTop >= margin
      ? 'top'
      : 'bottom';
  const rawTop = placement === 'top' ? preferredTop : rect.y + rect.height + margin;
  const rawLeft = rect.x;

  return {
    left: clamp(rawLeft, margin, viewport.width - bubble.width - margin),
    top: clamp(rawTop, margin, viewport.height - bubble.height - margin),
    placement
  };
}

export function getCenteredGuideBubblePlacement(
  viewport: ViewportSize,
  bubble: BubbleSize
): GuideBubblePlacement {
  return {
    left: clamp((viewport.width - bubble.width) / 2, margin, viewport.width - bubble.width - margin),
    top: clamp((viewport.height - bubble.height) / 2, margin, viewport.height - bubble.height - margin),
    placement: 'top'
  };
}

function clamp(value: number, min: number, max: number): number {
  if (max < min) return min;
  return Math.min(Math.max(value, min), max);
}
