import { describe, expect, it } from 'vitest';
import {
  calculateContainedViewport,
  DEFAULT_RECORDING_VIEWPORT,
  mapRectToContainedViewport,
  mapRectToFilledViewport,
  normalizeViewport
} from './viewportScaling';

describe('viewport scaling', () => {
  it('keeps the recorded aspect ratio on wider playback screens', () => {
    const placement = calculateContainedViewport(
      DEFAULT_RECORDING_VIEWPORT,
      { width: 1920, height: 1080 }
    );

    expect(placement.scale).toBeCloseTo(1.2);
    expect(placement.width).toBeCloseTo(1728);
    expect(placement.height).toBeCloseTo(1080);
    expect(placement.left).toBeCloseTo(96);
    expect(placement.top).toBeCloseTo(0);
  });

  it('letterboxes the same viewport on a narrower screen without reflow', () => {
    const placement = calculateContainedViewport(
      DEFAULT_RECORDING_VIEWPORT,
      { width: 1024, height: 768 }
    );

    expect(placement.scale).toBeCloseTo(1024 / 1440);
    expect(placement.width).toBeCloseTo(1024);
    expect(placement.height).toBeCloseTo(640);
    expect(placement.left).toBeCloseTo(0);
    expect(placement.top).toBeCloseTo(64);
  });

  it('maps a recorded element rectangle into the scaled content area', () => {
    const mapped = mapRectToContainedViewport(
      { x: 100, y: 200, width: 240, height: 60 },
      DEFAULT_RECORDING_VIEWPORT,
      { width: 1024, height: 768 }
    );

    expect(mapped.left).toBeCloseTo(100 * (1024 / 1440));
    expect(mapped.top).toBeCloseTo(64 + 200 * (1024 / 1440));
    expect(mapped.width).toBeCloseTo(240 * (1024 / 1440));
    expect(mapped.height).toBeCloseTo(60 * (1024 / 1440));
  });

  it('maps a recorded rectangle across the entire fullscreen viewport', () => {
    const mapped = mapRectToFilledViewport(
      { x: 100, y: 200, width: 240, height: 60 },
      DEFAULT_RECORDING_VIEWPORT,
      { width: 1920, height: 1080 }
    );

    expect(mapped.left).toBeCloseTo(100 * (1920 / 1440));
    expect(mapped.top).toBeCloseTo(200 * (1080 / 900));
    expect(mapped.width).toBeCloseTo(240 * (1920 / 1440));
    expect(mapped.height).toBeCloseTo(60 * (1080 / 900));
  });

  it('rejects invalid viewport metadata and can use a safe fallback', () => {
    expect(normalizeViewport({ width: 0, height: 900 })).toBeUndefined();
    expect(
      normalizeViewport(
        { width: Number.NaN, height: 900 },
        DEFAULT_RECORDING_VIEWPORT
      )
    ).toEqual(DEFAULT_RECORDING_VIEWPORT);
  });
});
