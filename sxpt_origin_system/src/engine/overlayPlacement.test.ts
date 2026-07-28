import { describe, expect, it } from 'vitest';
import {
  getCenteredGuideBubblePlacement,
  getGuideBubblePlacement,
  getGuideBubbleSize
} from './overlayPlacement';

describe('overlayPlacement', () => {
  it('places the guide bubble above the target when there is enough space', () => {
    const placement = getGuideBubblePlacement(
      { x: 240, y: 260, width: 120, height: 40 },
      { width: 900, height: 700 },
      { width: 280, height: 150 }
    );

    expect(placement).toEqual({
      left: 240,
      top: 98,
      placement: 'top'
    });
  });

  it('places the guide bubble below the target when the target is near the top', () => {
    const placement = getGuideBubblePlacement(
      { x: 240, y: 24, width: 120, height: 40 },
      { width: 900, height: 700 },
      { width: 280, height: 150 }
    );

    expect(placement).toEqual({
      left: 240,
      top: 76,
      placement: 'bottom'
    });
  });

  it('keeps the guide bubble inside the right and bottom edges', () => {
    const placement = getGuideBubblePlacement(
      { x: 820, y: 650, width: 80, height: 40 },
      { width: 900, height: 700 },
      { width: 280, height: 150 }
    );

    expect(placement).toEqual({
      left: 608,
      top: 488,
      placement: 'top'
    });
  });

  it('keeps a tall guide bubble usable inside a short viewport', () => {
    const placement = getGuideBubblePlacement(
      { x: 20, y: 520, width: 120, height: 40 },
      { width: 640, height: 560 },
      { width: 320, height: 620 }
    );

    expect(placement).toEqual({
      left: 20,
      top: 12,
      placement: 'top'
    });
  });

  it('centers a standalone guide bubble inside the viewport', () => {
    expect(
      getCenteredGuideBubblePlacement(
        { width: 900, height: 700 },
        { width: 320, height: 180 }
      )
    ).toEqual({ left: 290, top: 260, placement: 'top' });
  });

  it('uses a 1200px target width when the guide contains an image', () => {
    expect(getGuideBubbleSize(false, false)).toEqual({
      width: 320,
      height: 178
    });
    expect(getGuideBubbleSize(true, false)).toEqual({
      width: 280,
      height: 132
    });
    expect(getGuideBubbleSize(false, true)).toEqual({
      width: 1200,
      height: 820
    });
    expect(getGuideBubbleSize(true, true)).toEqual({
      width: 1200,
      height: 780
    });
  });

  it('pins an oversized image guide bubble to the safe viewport margin', () => {
    expect(
      getCenteredGuideBubblePlacement(
        { width: 640, height: 560 },
        getGuideBubbleSize(false, true)
      )
    ).toEqual({ left: 12, top: 12, placement: 'top' });
  });

  it('pins an oversized element-anchored image bubble to the safe margin', () => {
    expect(
      getGuideBubblePlacement(
        { x: 420, y: 300, width: 120, height: 40 },
        { width: 640, height: 560 },
        getGuideBubbleSize(false, true)
      )
    ).toEqual({ left: 12, top: 12, placement: 'top' });
  });
});
