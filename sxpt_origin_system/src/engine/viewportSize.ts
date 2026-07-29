import { onScopeDispose, readonly, shallowRef } from 'vue';
import type { ViewportSize } from './overlayPlacement';

export interface ViewportSource {
  innerWidth: number;
  innerHeight: number;
  addEventListener: (type: 'resize', listener: () => void) => void;
  removeEventListener: (type: 'resize', listener: () => void) => void;
}

export function useViewportSize(
  source: ViewportSource | undefined = browserViewportSource()
) {
  const viewport = shallowRef<ViewportSize>(readViewport(source));

  if (!source) {
    return readonly(viewport);
  }

  const updateViewport = () => {
    viewport.value = readViewport(source);
  };

  source.addEventListener('resize', updateViewport);
  onScopeDispose(() => source.removeEventListener('resize', updateViewport));

  return readonly(viewport);
}

function readViewport(source?: ViewportSource): ViewportSize {
  return source
    ? { width: source.innerWidth, height: source.innerHeight }
    : { width: 0, height: 0 };
}

function browserViewportSource(): ViewportSource | undefined {
  if (typeof window === 'undefined') return undefined;

  return {
    get innerWidth() {
      return window.innerWidth;
    },
    get innerHeight() {
      return window.innerHeight;
    },
    addEventListener: (_type, listener) =>
      window.addEventListener('resize', listener),
    removeEventListener: (_type, listener) =>
      window.removeEventListener('resize', listener)
  };
}
