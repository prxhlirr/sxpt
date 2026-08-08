import { describe, expect, it } from 'vitest';
import {
  beginOverlayDrag,
  clampOverlayPosition,
  updateOverlayDrag
} from './draggableOverlay';

describe('clampOverlayPosition', () => {
  it('keeps a dragged overlay inside an eight pixel viewport margin', () => {
    expect(
      clampOverlayPosition(
        { x: 900, y: -20 },
        { width: 300, height: 200 },
        { width: 1000, height: 700 }
      )
    ).toEqual({ x: 692, y: 8 });
  });

  it('anchors an oversized overlay at the visible margin', () => {
    expect(
      clampOverlayPosition(
        { x: 100, y: 100 },
        { width: 1200, height: 800 },
        { width: 1000, height: 700 }
      )
    ).toEqual({ x: 8, y: 8 });
  });
});

describe('overlay drag session', () => {
  it('does not move before the pointer crosses the drag threshold', () => {
    const session = beginOverlayDrag(
      7,
      { x: 100, y: 100 },
      { x: 20, y: 30 }
    );

    expect(
      updateOverlayDrag(
        session,
        7,
        { x: 102, y: 101 },
        { width: 300, height: 200 },
        { width: 1000, height: 700 }
      ).position
    ).toBeNull();
  });

  it('moves and clamps after the pointer crosses the drag threshold', () => {
    const session = beginOverlayDrag(
      7,
      { x: 100, y: 100 },
      { x: 680, y: 30 }
    );

    expect(
      updateOverlayDrag(
        session,
        7,
        { x: 140, y: 80 },
        { width: 300, height: 200 },
        { width: 1000, height: 700 }
      )
    ).toMatchObject({ moved: true, position: { x: 692, y: 10 } });
  });
});
