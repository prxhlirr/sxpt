import type { RecordedStep, Rect } from '../types/domain';
import { withRuntimeRect } from './runtimeStepTarget';

export interface VisibleOverlayStepInput {
  step?: RecordedStep;
  stepUrl?: string;
  frameReady: boolean;
  currentUrl: string;
  runtimeRect?: Rect;
}

export function getVisibleOverlayStep({
  step,
  stepUrl,
  frameReady,
  currentUrl,
  runtimeRect
}: VisibleOverlayStepInput): RecordedStep | undefined {
  if (
    !step ||
    !frameReady ||
    !isSameBusinessPage(currentUrl, stepUrl || step.url)
  ) {
    return undefined;
  }

  if (step.kind === 'guide' && step.anchor?.mode !== 'element') {
    return step;
  }

  return runtimeRect ? withRuntimeRect(step, runtimeRect) : undefined;
}

export function shouldForceBoundFrameReload(
  boundSrc: string,
  currentUrl: string,
  targetUrl: string,
  frameReady = true
): boolean {
  return Boolean(
    isSameBusinessPage(boundSrc, targetUrl) &&
      (!frameReady ||
        (currentUrl && !isSameBusinessPage(currentUrl, targetUrl)))
  );
}

export function isSameBusinessPage(left: string, right: string): boolean {
  if (!left || !right) return false;
  try {
    const base = 'http://business.local';
    const leftUrl = new URL(left, base);
    const rightUrl = new URL(right, base);
    return (
      leftUrl.pathname + leftUrl.search + leftUrl.hash ===
      rightUrl.pathname + rightUrl.search + rightUrl.hash
    );
  } catch {
    return left === right;
  }
}
