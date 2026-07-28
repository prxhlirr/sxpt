import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import { afterEach, describe, expect, it, vi } from 'vitest';

const recorderScript = readFileSync(
  new URL('../../public/training-recorder.js', import.meta.url),
  'utf8'
);

function createRuntimeHarness(options: { clampRetryTimers?: boolean } = {}) {
  let targetAvailable = false;
  let targetClicks = 0;
  const parentMessages: any[] = [];
  const windowListeners = new Map<string, (event: any) => void>();
  const parentWindow = {
    postMessage(message: unknown) {
      parentMessages.push(message);
    }
  };
  const target = {
    scrollIntoView() {},
    click() {
      targetClicks += 1;
    },
    dispatchEvent() {},
    getBoundingClientRect() {
      return { x: 10, y: 20, width: 120, height: 36 };
    }
  };
  const windowObject: Record<string, any> = {
    parent: parentWindow,
    location: { pathname: '/app', search: '', hash: '#/approve' },
    history: {},
    crypto: { randomUUID: () => 'sdk-test' },
    innerWidth: 1280,
    innerHeight: 720,
    devicePixelRatio: 1,
    addEventListener(type: string, listener: (event: any) => void) {
      windowListeners.set(type, listener);
    },
    setTimeout(callback: () => void, delay = 0) {
      const effectiveDelay =
        options.clampRetryTimers && delay === 100 ? 1000 : delay;
      return setTimeout(callback, effectiveDelay);
    },
    clearTimeout,
    requestAnimationFrame(callback: (timestamp: number) => void) {
      callback(0);
      return 1;
    }
  };
  function applyUrl(url?: string) {
    if (!url) return;
    const hashIndex = url.indexOf('#');
    if (hashIndex >= 0) windowObject.location.hash = url.slice(hashIndex);
  }
  windowObject.history.pushState = (_state: unknown, _title: string, url?: string) =>
    applyUrl(url);
  windowObject.history.replaceState = (_state: unknown, _title: string, url?: string) =>
    applyUrl(url);

  const documentObject = {
    currentScript: { dataset: {} },
    title: '异步审批页',
    addEventListener() {},
    querySelectorAll(selector: string) {
      return targetAvailable && selector === '#late-target' ? [target] : [];
    }
  };

  runInNewContext(recorderScript, {
    window: windowObject,
    document: documentObject,
    console,
    Date,
    Math,
    Number,
    Array,
    String,
    Boolean,
    Event: class {}
  });
  parentMessages.length = 0;

  return {
    parentMessages,
    setTargetAvailable(value: boolean) {
      targetAvailable = value;
    },
    sendParentMessage(data: unknown) {
      windowListeners.get('message')?.({
        source: parentWindow,
        origin: 'http://teach.local',
        data
      });
    },
    navigate(url: string) {
      windowObject.history.pushState({}, '', url);
    },
    getTargetClicks() {
      return targetClicks;
    }
  };
}

describe('training recorder runtime', () => {
  afterEach(() => vi.useRealTimers());

  it('waits for a delayed SPA target before reporting resolution failure', async () => {
    vi.useFakeTimers();
    const runtime = createRuntimeHarness();

    runtime.sendParentMessage({
      type: 'RESOLVE_RECORDED_TARGET',
      requestId: 'resolve-late-target',
      locator: { selector: '#late-target' }
    });
    runtime.setTargetAvailable(true);
    await vi.advanceTimersByTimeAsync(120);

    expect(runtime.parentMessages).toContainEqual(
      expect.objectContaining({
        type: 'TARGET_RESOLUTION_RESULT',
        requestId: 'resolve-late-target',
        success: true,
        url: '/app#/approve'
      })
    );
    expect(runtime.parentMessages).not.toContainEqual(
      expect.objectContaining({
        type: 'TARGET_RESOLUTION_RESULT',
        requestId: 'resolve-late-target',
        success: false
      })
    );
  });

  it('does not execute a stale delayed playback after the SPA route changes', async () => {
    vi.useFakeTimers();
    const runtime = createRuntimeHarness();

    runtime.sendParentMessage({
      type: 'PLAY_RECORDED_STEP',
      requestId: 'play-old-route',
      step: { selector: '#late-target', actionType: 'click' }
    });
    runtime.navigate('#/another-route');
    runtime.setTargetAvailable(true);
    await vi.advanceTimersByTimeAsync(250);

    expect(runtime.getTargetClicks()).toBe(0);
    expect(runtime.parentMessages).not.toContainEqual(
      expect.objectContaining({
        type: 'PLAYBACK_RESULT',
        requestId: 'play-old-route',
        success: true
      })
    );
  });

  it('keeps waiting near the parent request deadline for a slow SPA target', async () => {
    vi.useFakeTimers();
    const runtime = createRuntimeHarness();

    runtime.sendParentMessage({
      type: 'RESOLVE_RECORDED_TARGET',
      requestId: 'resolve-slow-target',
      locator: { selector: '#late-target' }
    });
    await vi.advanceTimersByTimeAsync(4200);
    runtime.setTargetAvailable(true);
    await vi.advanceTimersByTimeAsync(150);

    expect(runtime.parentMessages).toContainEqual(
      expect.objectContaining({
        type: 'TARGET_RESOLUTION_RESULT',
        requestId: 'resolve-slow-target',
        success: true
      })
    );
  });

  it('never executes delayed playback after the absolute request deadline', async () => {
    vi.useFakeTimers();
    const runtime = createRuntimeHarness({ clampRetryTimers: true });

    runtime.sendParentMessage({
      type: 'PLAY_RECORDED_STEP',
      requestId: 'play-expired-target',
      step: { selector: '#late-target', actionType: 'click' }
    });
    await vi.advanceTimersByTimeAsync(5100);
    runtime.setTargetAvailable(true);
    await vi.advanceTimersByTimeAsync(1200);

    expect(runtime.getTargetClicks()).toBe(0);
  });
});
