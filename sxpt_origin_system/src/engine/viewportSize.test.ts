import { effectScope } from 'vue';
import { describe, expect, it } from 'vitest';
import {
  useViewportSize,
  type ViewportSource
} from './viewportSize';

describe('useViewportSize', () => {
  it('tracks resize events and removes its listener when the scope stops', () => {
    const resizeListeners = new Set<() => void>();
    const source: ViewportSource = {
      innerWidth: 1600,
      innerHeight: 900,
      addEventListener: (_type, listener) => resizeListeners.add(listener),
      removeEventListener: (_type, listener) => resizeListeners.delete(listener)
    };
    const scope = effectScope();
    let viewport: ReturnType<typeof useViewportSize> | undefined;

    scope.run(() => {
      viewport = useViewportSize(source);
    });

    expect(viewport?.value).toEqual({ width: 1600, height: 900 });
    expect(resizeListeners.size).toBe(1);

    source.innerWidth = 640;
    source.innerHeight = 560;
    resizeListeners.forEach((listener) => listener());

    expect(viewport?.value).toEqual({ width: 640, height: 560 });

    scope.stop();
    expect(resizeListeners.size).toBe(0);
  });
});
